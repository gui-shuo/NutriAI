#!/bin/bash
set -euo pipefail

# 旧库 nutriai -> 腾讯云新库迁移脚本
# 用法：
#   1. 先在环境变量中配置 SOURCE_DB_* 与 TARGET_DB_* / DB_HOST_NEI 等参数
#   2. 显式确认后执行：
#      CONFIRM_DESTROY_TARGET=YES ./migrate-db-to-tencent.sh
# 说明：
#   - 该脚本会重建目标库，再导入源库 dump
#   - 导入完成后会分别导出源库/目标库并做 SHA256 对比
#   - 只有校验通过才视为“100%一致”

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok() { echo -e "${GREEN}[OK]${NC}    $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

require_cmd() {
    command -v "$1" >/dev/null 2>&1 || {
        log_error "缺少命令: $1"
        exit 1
    }
}

require_env() {
    local key="$1"
    local value="${!key:-}"
    if [ -z "$value" ]; then
        log_error "缺少环境变量: $key"
        exit 1
    fi
}

mysql_exec() {
    local host="$1"
    local port="$2"
    local user="$3"
    local password="$4"
    shift 4
    MYSQL_PWD="$password" mysql --host="$host" --port="$port" --user="$user" --default-character-set=utf8mb4 "$@"
}

mysqldump_exec() {
    local host="$1"
    local port="$2"
    local user="$3"
    local password="$4"
    shift 4
    MYSQL_PWD="$password" mysqldump \
        --host="$host" \
        --port="$port" \
        --user="$user" \
        --default-character-set=utf8mb4 \
        --single-transaction \
        --routines \
        --events \
        --triggers \
        --hex-blob \
        --order-by-primary \
        --skip-comments \
        --skip-dump-date \
        --set-gtid-purged=OFF \
        "$@"
}

normalize_dump() {
    local input_file="$1"
    local output_file="$2"
    local db_name="$3"
    sed \
        -e "s/CREATE DATABASE .*\`$db_name\`/CREATE DATABASE IF NOT EXISTS \`__NUTRIAI_DB__\`/g" \
        -e "s/USE \`$db_name\`/USE \`__NUTRIAI_DB__\`/g" \
        "$input_file" > "$output_file"
}

require_cmd mysql
require_cmd mysqldump
require_cmd sha256sum

require_env SOURCE_DB_HOST
require_env SOURCE_DB_PORT
require_env SOURCE_DB_NAME
require_env SOURCE_DB_USERNAME
require_env SOURCE_DB_PASSWORD
require_env TARGET_DB_USERNAME
require_env TARGET_DB_PASSWORD

TARGET_DB_HOST="${TARGET_DB_HOST:-${DB_HOST_NEI:-}}"
TARGET_DB_PORT="${TARGET_DB_PORT:-${DB_PORT_NEI:-3306}}"
TARGET_DB_NAME="${TARGET_DB_NAME:-${DB_NAME:-nutriai}}"

if [ -z "$TARGET_DB_HOST" ]; then
    log_error "缺少 TARGET_DB_HOST 或 DB_HOST_NEI"
    exit 1
fi

if [ "${CONFIRM_DESTROY_TARGET:-}" != "YES" ]; then
    log_error "该脚本会清空并重建目标库，请先设置 CONFIRM_DESTROY_TARGET=YES"
    exit 1
fi

WORK_DIR="${WORK_DIR:-$(mktemp -d)}"
SOURCE_DUMP="$WORK_DIR/source.sql"
TARGET_DUMP="$WORK_DIR/target.sql"
SOURCE_NORMALIZED="$WORK_DIR/source.normalized.sql"
TARGET_NORMALIZED="$WORK_DIR/target.normalized.sql"
SOURCE_SHA_FILE="$WORK_DIR/source.sha256"
TARGET_SHA_FILE="$WORK_DIR/target.sha256"

log_info "导出旧库 ${SOURCE_DB_NAME} (${SOURCE_DB_HOST}:${SOURCE_DB_PORT}) ..."
mysqldump_exec \
    "$SOURCE_DB_HOST" \
    "$SOURCE_DB_PORT" \
    "$SOURCE_DB_USERNAME" \
    "$SOURCE_DB_PASSWORD" \
    --databases "$SOURCE_DB_NAME" > "$SOURCE_DUMP"

log_info "重建目标库 ${TARGET_DB_NAME} (${TARGET_DB_HOST}:${TARGET_DB_PORT}) ..."
mysql_exec \
    "$TARGET_DB_HOST" \
    "$TARGET_DB_PORT" \
    "$TARGET_DB_USERNAME" \
    "$TARGET_DB_PASSWORD" \
    -e "DROP DATABASE IF EXISTS \`${TARGET_DB_NAME}\`; CREATE DATABASE \`${TARGET_DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

log_info "导入目标库 ..."
mysql_exec \
    "$TARGET_DB_HOST" \
    "$TARGET_DB_PORT" \
    "$TARGET_DB_USERNAME" \
    "$TARGET_DB_PASSWORD" < "$SOURCE_DUMP"

log_info "导出目标库用于校验 ..."
mysqldump_exec \
    "$TARGET_DB_HOST" \
    "$TARGET_DB_PORT" \
    "$TARGET_DB_USERNAME" \
    "$TARGET_DB_PASSWORD" \
    --databases "$TARGET_DB_NAME" > "$TARGET_DUMP"

log_info "标准化 dump 并计算校验值 ..."
normalize_dump "$SOURCE_DUMP" "$SOURCE_NORMALIZED" "$SOURCE_DB_NAME"
normalize_dump "$TARGET_DUMP" "$TARGET_NORMALIZED" "$TARGET_DB_NAME"
sha256sum "$SOURCE_NORMALIZED" | tee "$SOURCE_SHA_FILE"
sha256sum "$TARGET_NORMALIZED" | tee "$TARGET_SHA_FILE"

SOURCE_SHA="$(cut -d' ' -f1 "$SOURCE_SHA_FILE")"
TARGET_SHA="$(cut -d' ' -f1 "$TARGET_SHA_FILE")"

if [ "$SOURCE_SHA" != "$TARGET_SHA" ]; then
    log_error "迁移校验失败：源库与目标库 SHA256 不一致"
    log_error "源库: $SOURCE_SHA"
    log_error "目标库: $TARGET_SHA"
    exit 1
fi

log_ok "数据库迁移完成，源库与目标库 SHA256 完全一致"
log_info "工作目录: $WORK_DIR"
