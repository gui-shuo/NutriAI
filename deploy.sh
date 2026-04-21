#!/bin/bash
#===============================================================================
# NutriAI 阿里云 ACR 服务器部署脚本
# 说明:
#   1. 后端与前端镜像由阿里云 ACR 个人版自动从 GitHub 仓库构建
#   2. 本脚本运行在服务器上，自动解析最新公共时间戳镜像并用 docker compose 启动
# 用法:
#   ./deploy.sh deploy                 # 自动查找最新公共时间戳镜像并部署
#   ./deploy.sh deploy 20260421123045 # 部署指定时间戳镜像
#   ./deploy.sh resolve-tag           # 输出当前最新公共时间戳 tag
#   ./deploy.sh status                # 查看服务状态
#   ./deploy.sh logs backend          # 查看指定服务日志
#   ./deploy.sh healthcheck           # 简单健康检查
#===============================================================================
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.prod.yml}"
DEPLOY_PROJECT="${DEPLOY_PROJECT:-nutriai}"
IMAGE_TAG_REGEX="${IMAGE_TAG_REGEX:-^[0-9]{14}$}"
ALLOW_LATEST_FALLBACK="${ALLOW_LATEST_FALLBACK:-false}"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

load_env_file() {
    if [ ! -f .env ]; then
        log_error "当前目录缺少 .env 文件，请先参考 .env.example 创建运行时配置。"
        exit 1
    fi

    set -a
    while IFS= read -r line || [ -n "$line" ]; do
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        export "$line" 2>/dev/null || true
    done < .env
    set +a
}

initialize_config() {
    load_env_file
    ACR_REGISTRY="${ACR_REGISTRY:-crpi-lds0nj6fmrvtaiga.cn-beijing.personal.cr.aliyuncs.com}"
    ACR_NAMESPACE="${ACR_NAMESPACE:-heyitshuo1}"
    ACR_USERNAME="${ACR_USERNAME:-}"
    ACR_PASSWORD="${ACR_PASSWORD:-}"
    BACKEND_IMAGE_REPO="${BACKEND_IMAGE_REPO:-nutriai-backend}"
    FRONTEND_IMAGE_REPO="${FRONTEND_IMAGE_REPO:-nutriai-frontend}"
    REDIS_IMAGE="${REDIS_IMAGE:-registry.cn-hangzhou.aliyuncs.com/acs/redis:6.0-alpine}"
}

require_cmd() {
    local cmd="$1"
    if ! command -v "$cmd" >/dev/null 2>&1; then
        log_error "缺少依赖命令: $cmd"
        exit 1
    fi
}

require_env() {
    local key="$1"
    if [ -z "${!key:-}" ]; then
        log_error ".env 中缺少必填项: $key"
        exit 1
    fi
}

upsert_env() {
    local key="$1"
    local value="$2"
    if grep -q "^${key}=" .env 2>/dev/null; then
        sed -i "s|^${key}=.*|${key}=${value}|" .env
    else
        printf '%s=%s\n' "$key" "$value" >> .env
    fi
}

compose() {
    if docker compose version >/dev/null 2>&1; then
        docker compose --env-file .env -p "$DEPLOY_PROJECT" -f "$COMPOSE_FILE" "$@"
    else
        docker-compose --env-file .env -p "$DEPLOY_PROJECT" -f "$COMPOSE_FILE" "$@"
    fi
}

docker_login() {
    log_info "登录阿里云 ACR..."
    echo "$ACR_PASSWORD" | docker login "$ACR_REGISTRY" -u "$ACR_USERNAME" --password-stdin >/dev/null
    log_ok "阿里云 ACR 登录成功"
}

docker_logout() {
    docker logout "$ACR_REGISTRY" >/dev/null 2>&1 || true
}

fetch_repo_tags() {
    local repo="$1"
    local output_file="$2"
    curl -fsSL \
        --retry 3 \
        --retry-delay 2 \
        --retry-connrefused \
        -u "${ACR_USERNAME}:${ACR_PASSWORD}" \
        "https://${ACR_REGISTRY}/v2/${ACR_NAMESPACE}/${repo}/tags/list" \
        -o "$output_file"
}

resolve_latest_common_tag() {
    local backend_tags_file frontend_tags_file latest_tag
    backend_tags_file="$(mktemp)"
    frontend_tags_file="$(mktemp)"

    fetch_repo_tags "$BACKEND_IMAGE_REPO" "$backend_tags_file"
    fetch_repo_tags "$FRONTEND_IMAGE_REPO" "$frontend_tags_file"

    if ! latest_tag="$(python3 - "$backend_tags_file" "$frontend_tags_file" "$IMAGE_TAG_REGEX" <<'PY'
import json
import re
import sys

with open(sys.argv[1], encoding="utf-8") as backend_fp:
    backend = json.load(backend_fp)
with open(sys.argv[2], encoding="utf-8") as frontend_fp:
    frontend = json.load(frontend_fp)

pattern = re.compile(sys.argv[3])
backend_tags = {tag for tag in backend.get("tags") or [] if pattern.fullmatch(tag)}
frontend_tags = {tag for tag in frontend.get("tags") or [] if pattern.fullmatch(tag)}
common_tags = sorted(backend_tags & frontend_tags, reverse=True)

if not common_tags:
    sys.exit(1)

print(common_tags[0])
PY
    )"; then
        rm -f "$backend_tags_file" "$frontend_tags_file"
        return 1
    fi

    rm -f "$backend_tags_file" "$frontend_tags_file"
    echo "$latest_tag"
}

resolve_image_tag() {
    if [ -n "${IMAGE_TAG:-}" ]; then
        echo "$IMAGE_TAG"
        return 0
    fi

    local latest_tag
    if latest_tag="$(resolve_latest_common_tag)"; then
        echo "$latest_tag"
        return 0
    fi

    if [ "$ALLOW_LATEST_FALLBACK" = "true" ]; then
        log_warn "未找到符合 ${IMAGE_TAG_REGEX} 的公共时间戳 tag，回退到 latest。"
        echo "latest"
        return 0
    fi

    log_error "未找到同时存在于 ${BACKEND_IMAGE_REPO} 和 ${FRONTEND_IMAGE_REPO} 的时间戳 tag。"
    log_error "请在 ACR 控制台确认镜像版本命名使用 14 位时间戳，或手动执行 IMAGE_TAG=具体标签 ./deploy.sh deploy。"
    exit 1
}

cleanup_old_app_images() {
    local current_tag="$1"
    local backend_repo frontend_repo
    backend_repo="${ACR_REGISTRY}/${ACR_NAMESPACE}/${BACKEND_IMAGE_REPO}"
    frontend_repo="${ACR_REGISTRY}/${ACR_NAMESPACE}/${FRONTEND_IMAGE_REPO}"

    docker image ls "$backend_repo" --format '{{.Repository}}:{{.Tag}}' | grep -v ":${current_tag}$" | xargs -r docker rmi -f >/dev/null 2>&1 || true
    docker image ls "$frontend_repo" --format '{{.Repository}}:{{.Tag}}' | grep -v ":${current_tag}$" | xargs -r docker rmi -f >/dev/null 2>&1 || true
    docker image prune -f >/dev/null 2>&1 || true
}

do_resolve_tag() {
    initialize_config
    require_cmd curl
    require_cmd python3
    require_env ACR_REGISTRY
    require_env ACR_NAMESPACE
    require_env ACR_USERNAME
    require_env ACR_PASSWORD
    resolve_image_tag
}

do_deploy() {
    initialize_config
    require_cmd curl
    require_cmd docker
    require_cmd python3
    require_env ACR_REGISTRY
    require_env ACR_NAMESPACE
    require_env ACR_USERNAME
    require_env ACR_PASSWORD

    if [ -n "${1:-}" ]; then
        IMAGE_TAG="$1"
    fi

    local target_tag
    target_tag="$(resolve_image_tag)"
    log_info "准备部署镜像标签: ${target_tag}"

    upsert_env IMAGE_TAG "$target_tag"
    upsert_env ACR_REGISTRY "$ACR_REGISTRY"
    upsert_env ACR_NAMESPACE "$ACR_NAMESPACE"
    upsert_env BACKEND_IMAGE_REPO "$BACKEND_IMAGE_REPO"
    upsert_env FRONTEND_IMAGE_REPO "$FRONTEND_IMAGE_REPO"
    upsert_env REDIS_IMAGE "$REDIS_IMAGE"
    chmod 600 .env || true

    docker_login
    compose pull redis backend frontend
    compose up -d --force-recreate --remove-orphans
    compose ps
    cleanup_old_app_images "$target_tag"
    docker_logout

    log_ok "部署完成！(标签: ${target_tag})"
}

do_status() {
    initialize_config
    compose ps
}

do_logs() {
    initialize_config
    local service="${1:-backend}"
    compose logs --tail=100 -f "$service"
}

do_healthcheck() {
    log_info "后端健康检查..."
    curl -sf http://localhost:8080/api/health >/dev/null && log_ok "后端正常" || log_warn "后端接口未通过健康检查"
    log_info "前端健康检查..."
    curl -sf http://localhost:3000 >/dev/null && log_ok "前端正常" || log_warn "前端首页未通过健康检查"
}

case "${1:-deploy}" in
    deploy)
        shift || true
        do_deploy "${1:-}"
        ;;
    resolve-tag)
        do_resolve_tag
        ;;
    status)
        do_status
        ;;
    logs)
        shift || true
        do_logs "${1:-backend}"
        ;;
    health|healthcheck)
        do_healthcheck
        ;;
    *)
        echo "用法: $0 {deploy [IMAGE_TAG]|resolve-tag|status|logs [service]|healthcheck}"
        exit 1
        ;;
esac
