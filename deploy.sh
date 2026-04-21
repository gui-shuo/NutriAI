#!/bin/bash
#===============================================================================
# NutriAI 腾讯云部署辅助脚本
# 说明:
#   1. 镜像构建与推送统一在 GitHub Actions 中手动触发完成
#   2. 本脚本仅保留腾讯云服务器运维能力，不再执行本地构建/推送
# 用法:
#   IMAGE_TAG=20260419123045 ./deploy.sh deploy   # 部署指定时间戳镜像到腾讯云服务器
#   ./deploy.sh status                            # 查看腾讯云服务器服务状态
#   ./deploy.sh logs backend                      # 查看腾讯云服务器容器日志
#   ./deploy.sh rollback                         # 使用服务器当前 compose 配置重启容器
#===============================================================================
set -euo pipefail

# ---- 项目根目录 ----
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

# ---- 加载环境变量 ----
if [ -f .env ]; then
    set -a
    # 处理多行 SSH Key：跳过它，只加载普通变量
    while IFS= read -r line || [[ -n "$line" ]]; do
        # 跳过空行和注释
        [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]] && continue
        # 遇到 SSH Key 开始，跳到结束
        if [[ "$line" =~ ^(TENCENT_SERVER_SSH_KEY|SERVER_SSH_KEY|VOLCENGINE_SSH_KEY)= ]]; then
            while IFS= read -r line; do
                [[ "$line" == *"END OPENSSH PRIVATE KEY"* ]] && break
            done
            continue
        fi
        # 普通变量
        export "$line" 2>/dev/null || true
    done < .env
    set +a
fi

# ---- 构建配置 ----
export DOCKER_BUILDKIT="${DOCKER_BUILDKIT:-1}"
export COMPOSE_DOCKER_CLI_BUILD="${COMPOSE_DOCKER_CLI_BUILD:-1}"
PROXY_URL="${PROXY_URL:-http://127.0.0.1:7897}"
export HTTP_PROXY="${HTTP_PROXY:-${PROXY_URL}}"
export HTTPS_PROXY="${HTTPS_PROXY:-${PROXY_URL}}"
export ALL_PROXY="${ALL_PROXY:-${PROXY_URL}}"
export NO_PROXY="${NO_PROXY:-127.0.0.1,localhost}"
BASE_IMAGE_MIRROR="${BASE_IMAGE_MIRROR:-docker.m.daocloud.io}"

DOCKER_USER="${DOCKER_NAME:-heyitshuo}"
TCR_REGISTRY="${TCR_REGISTRY:-ccr.ccs.tencentyun.com}"
TCR_NAMESPACE="${TCR_NAMESPACE:-heyitshuo}"
APP_REGISTRY="${APP_REGISTRY:-${TCR_REGISTRY}}"
APP_NAMESPACE="${APP_NAMESPACE:-${TCR_NAMESPACE}}"
BACKEND_IMAGE="${APP_REGISTRY}/${APP_NAMESPACE}/nutriai-backend"
FRONTEND_IMAGE="${APP_REGISTRY}/${APP_NAMESPACE}/nutriai-frontend"
MAVEN_BUILDER_IMAGE="${TCR_REGISTRY}/${TCR_NAMESPACE}/base-maven:3.9-eclipse-temurin-17"
JAVA_RUNTIME_IMAGE="${TCR_REGISTRY}/${TCR_NAMESPACE}/base-eclipse-temurin:17-jre-alpine"
NODE_BUILDER_IMAGE="${TCR_REGISTRY}/${TCR_NAMESPACE}/base-node:20-alpine"
NGINX_RUNTIME_IMAGE="${TCR_REGISTRY}/${TCR_NAMESPACE}/base-nginx:1.25-alpine"
SERVER_HOST="${TENCENT_SERVER_HOST:-${SERVER_HOST:-${VOLCENGINE_IPV4:-}}}"
SERVER_USER="${TENCENT_SERVER_USER:-${SERVER_USER:-${VOLCENGINE_SSH_USER:-root}}}"
SERVER_DEPLOY_DIR="${SERVER_DEPLOY_DIR:-/www/wwwroot/nutriai}"
SSH_KEY_FILE=""

# ---- 颜色输出 ----
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

require_image_tag() {
    if [ -z "${IMAGE_TAG:-}" ] || [ "${IMAGE_TAG}" = "latest" ]; then
        log_error "请通过环境变量提供时间戳镜像标签，例如：IMAGE_TAG=20260419123045 ./deploy.sh deploy"
        exit 1
    fi
}

deny_local_build() {
    log_error "本地构建/推送已停用，请在 GitHub Actions 中手动执行 deploy workflow。"
    exit 1
}

# ---- 提取 SSH Key ----
setup_ssh_key() {
    SSH_KEY_FILE=$(mktemp /tmp/tencent_ssh_XXXXXX)
    python3 -c "
from pathlib import Path
content = Path('${PROJECT_DIR}/.env').read_text()
for key_name in ('TENCENT_SERVER_SSH_KEY=', 'SERVER_SSH_KEY=', 'VOLCENGINE_SSH_KEY='):
    idx = content.find(key_name)
    if idx != -1:
        key_part = content[idx + len(key_name):]
        key_part = key_part.lstrip('\"').lstrip(\"'\")
        end_idx = key_part.index('-----END OPENSSH PRIVATE KEY-----') + len('-----END OPENSSH PRIVATE KEY-----')
        key = key_part[:end_idx].strip()
        Path('${SSH_KEY_FILE}').write_text(key + '\n')
        import os; os.chmod('${SSH_KEY_FILE}', 0o600)
        break
else:
    raise ValueError('未在 .env 中找到服务器 SSH 私钥')
"
}

cleanup_ssh_key() {
    [ -n "$SSH_KEY_FILE" ] && rm -f "$SSH_KEY_FILE"
}
trap cleanup_ssh_key EXIT

# ---- SSH 封装 ----
ve_ssh() {
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15 -i "$SSH_KEY_FILE" "${SERVER_USER}@${SERVER_HOST}" "$@"
}

ve_scp() {
    scp -o StrictHostKeyChecking=no -o ConnectTimeout=15 -i "$SSH_KEY_FILE" "$@"
}

# ---- Docker Hub 登录 ----
docker_login() {
    log_info "登录 Docker Hub..."
    echo "${DOCKER_KEY}" | docker login -u "${DOCKER_USER}" --password-stdin 2>/dev/null
    log_ok "Docker Hub 登录成功"
}

# ---- 腾讯云 TCR 登录 ----
tcr_login() {
    log_info "登录腾讯云 TCR..."
    echo "${TCR_PASSWORD}" | docker login "${TCR_REGISTRY}" -u "${TCR_USERNAME}" --password-stdin 2>/dev/null
    log_ok "腾讯云 TCR 登录成功"
}

# ---- 生成时间戳标签 ----
generate_tag() {
    echo "$(date +%Y%m%d%H%M%S)"
}

# ---- 更新 docker-compose 文件中的镜像标签 ----
update_compose_tags() {
    local tag="$1"
    log_info "更新 docker-compose 文件中的镜像标签为 ${tag}..."
    for f in docker-compose.local.yml; do
        if [ -f "$f" ]; then
            sed -i -E "s|(^[[:space:]]*image:[[:space:]]*).*/nutriai-backend:[^[:space:]]+|\1${BACKEND_IMAGE}:${tag}|" "$f"
            sed -i -E "s|(^[[:space:]]*image:[[:space:]]*).*/nutriai-frontend:[^[:space:]]+|\1${FRONTEND_IMAGE}:${tag}|" "$f"
            log_ok "已更新 $f"
        fi
    done
}

# ---- 构建镜像 ----
do_build() {
    log_info "========== 构建 Docker 镜像 =========="
    log_info "BuildKit 已启用: DOCKER_BUILDKIT=${DOCKER_BUILDKIT}"
    tcr_login

    IMAGE_TAG=$(generate_tag)
    log_info "镜像标签: ${IMAGE_TAG}"

    log_info "构建后端镜像..."
    docker build --provenance=false --sbom=false \
        --build-arg MAVEN_BUILDER_IMAGE="${MAVEN_BUILDER_IMAGE}" \
        --build-arg JAVA_RUNTIME_IMAGE="${JAVA_RUNTIME_IMAGE}" \
        --build-arg HTTP_PROXY="${HTTP_PROXY}" \
        --build-arg HTTPS_PROXY="${HTTPS_PROXY}" \
        --build-arg ALL_PROXY="${ALL_PROXY}" \
        --build-arg NO_PROXY="${NO_PROXY}" \
        -t "${BACKEND_IMAGE}:${IMAGE_TAG}" ./backend
    log_ok "后端镜像构建完成"

    log_info "构建前端镜像..."
    docker build --provenance=false --sbom=false \
        --build-arg NODE_BUILDER_IMAGE="${NODE_BUILDER_IMAGE}" \
        --build-arg NGINX_RUNTIME_IMAGE="${NGINX_RUNTIME_IMAGE}" \
        --build-arg HTTP_PROXY="${HTTP_PROXY}" \
        --build-arg HTTPS_PROXY="${HTTPS_PROXY}" \
        --build-arg ALL_PROXY="${ALL_PROXY}" \
        --build-arg NO_PROXY="${NO_PROXY}" \
        -t "${FRONTEND_IMAGE}:${IMAGE_TAG}" ./frontend
    log_ok "前端镜像构建完成"

    # 更新 docker-compose 文件
    update_compose_tags "${IMAGE_TAG}"

    log_ok "所有镜像构建完成 (标签: ${IMAGE_TAG})"
    docker images | grep -E "(nutriai-backend|nutriai-frontend)" | head -5

    # 保存标签供后续步骤使用
    echo "${IMAGE_TAG}" > /tmp/nutriai_image_tag
}

# ---- 获取当前镜像标签 ----
get_current_tag() {
    require_image_tag
    echo "${IMAGE_TAG}"
}

# ---- 同步基础镜像到腾讯云 TCR ----
sync_base_image() {
    local source_image="$1"
    local target_image="$2"

    log_info "同步基础镜像: ${source_image} -> ${target_image}"
    HTTP_PROXY="${HTTP_PROXY}" HTTPS_PROXY="${HTTPS_PROXY}" ALL_PROXY="${ALL_PROXY}" NO_PROXY="${NO_PROXY}" docker pull "${source_image}"
    docker tag "${source_image}" "${target_image}"
    HTTP_PROXY="${HTTP_PROXY}" HTTPS_PROXY="${HTTPS_PROXY}" ALL_PROXY="${ALL_PROXY}" NO_PROXY="${NO_PROXY}" docker push "${target_image}"
    log_ok "基础镜像同步完成: ${target_image}"
}

sync_base_images() {
    log_info "========== 同步基础镜像到腾讯云 TCR =========="
    log_info "当前代理: ${HTTP_PROXY}"
    tcr_login

    sync_base_image "${BASE_IMAGE_MIRROR}/library/maven:3.9-eclipse-temurin-17" "${MAVEN_BUILDER_IMAGE}"
    sync_base_image "${BASE_IMAGE_MIRROR}/library/eclipse-temurin:17-jre-alpine" "${JAVA_RUNTIME_IMAGE}"
    sync_base_image "${BASE_IMAGE_MIRROR}/library/node:20-alpine" "${NODE_BUILDER_IMAGE}"
    sync_base_image "${BASE_IMAGE_MIRROR}/library/nginx:1.25-alpine" "${NGINX_RUNTIME_IMAGE}"

    log_ok "所有基础镜像已同步到腾讯云 TCR"
}

# ---- 推送镜像 ----
do_push() {
    log_info "========== 推送镜像到腾讯云 TCR =========="
    tcr_login

    IMAGE_TAG=$(get_current_tag)
    log_info "推送标签: ${IMAGE_TAG}"

    log_info "推送后端镜像..."
    docker push "${BACKEND_IMAGE}:${IMAGE_TAG}"
    log_ok "后端镜像推送完成"

    log_info "推送前端镜像..."
    docker push "${FRONTEND_IMAGE}:${IMAGE_TAG}"
    log_ok "前端镜像推送完成"

    log_ok "所有镜像推送完成 (标签: ${IMAGE_TAG})"
}

# ---- 部署到腾讯云服务器 ----
do_deploy() {
    log_info "========== 部署到腾讯云服务器 =========="
    setup_ssh_key

    IMAGE_TAG=$(get_current_tag)
    log_info "部署标签: ${IMAGE_TAG}"

    ve_ssh "mkdir -p ${SERVER_DEPLOY_DIR}"

    log_info "上传 docker-compose 配置..."
    ve_scp "${PROJECT_DIR}/docker-compose.prod.yml" "${SERVER_USER}@${SERVER_HOST}:${SERVER_DEPLOY_DIR}/docker-compose.prod.yml"

    log_info "在腾讯云服务器上拉取镜像并启动..."
    ve_ssh "
        set -e
        cd ${SERVER_DEPLOY_DIR}

        require_env() {
            local key=\"\$1\"
            local current_value
            current_value=\"\$(grep -E \"^\${key}=\" .env 2>/dev/null | tail -n 1 | cut -d= -f2-)\"
            if [ -z \"\$current_value\" ]; then
                echo \"ERROR: 服务器 .env 缺少 \${key} 配置\" >&2
                exit 1
            fi
        }

        upsert_env() {
            local key=\"\$1\"
            local value=\"\$2\"
            if grep -q \"^\${key}=\" .env 2>/dev/null; then
                sed -i \"s|^\${key}=.*|\${key}=\${value}|\" .env
            else
                printf '%s=%s\\n' \"\$key\" \"\$value\" >> .env
            fi
        }

        retry_pull() {
            local image=\"\$1\"
            local attempt=1
            while [ \"\$attempt\" -le 3 ]; do
                echo \"拉取镜像 \${image}，第 \${attempt} 次尝试...\"
                if docker pull \"\$image\"; then
                    return 0
                fi
                attempt=\$((attempt + 1))
                sleep 10
            done
            return 1
        }

        if [ ! -f .env ]; then
            echo 'ERROR: 服务器缺少 .env 文件，请先准备运行时配置' >&2
            exit 1
        fi

        require_env DB_HOST_NEI
        require_env DB_PORT_NEI
        require_env COS_URL

        upsert_env IMAGE_TAG '${IMAGE_TAG}'
        upsert_env TCR_REGISTRY '${TCR_REGISTRY}'
        upsert_env TCR_NAMESPACE '${TCR_NAMESPACE}'
        chmod 600 .env

        echo '${TCR_PASSWORD}' | docker login '${TCR_REGISTRY}' -u '${TCR_USERNAME}' --password-stdin

        COMPOSE_CMD='docker compose'
        if ! docker compose version >/dev/null 2>&1; then
            COMPOSE_CMD='docker-compose'
        fi

        BACKEND_IMAGE='${BACKEND_IMAGE}:${IMAGE_TAG}'
        FRONTEND_IMAGE='${FRONTEND_IMAGE}:${IMAGE_TAG}'

        retry_pull \"\$BACKEND_IMAGE\"
        retry_pull \"\$FRONTEND_IMAGE\"

        \$COMPOSE_CMD -p nutriai -f docker-compose.prod.yml down --remove-orphans || true
        docker container prune -f >/dev/null 2>&1 || true
        \$COMPOSE_CMD -p nutriai -f docker-compose.prod.yml up -d --force-recreate
        \$COMPOSE_CMD -p nutriai -f docker-compose.prod.yml ps

        docker image ls '${BACKEND_IMAGE}' --format '{{.Repository}}:{{.Tag}}' | grep -v ':${IMAGE_TAG}$' | xargs -r docker rmi -f || true
        docker image ls '${FRONTEND_IMAGE}' --format '{{.Repository}}:{{.Tag}}' | grep -v ':${IMAGE_TAG}$' | xargs -r docker rmi -f || true
        docker image prune -f >/dev/null 2>&1 || true
        docker logout '${TCR_REGISTRY}' || true
    "

    log_ok "部署完成！(标签: ${IMAGE_TAG})"
}

# ---- 查看状态 ----
do_status() {
    setup_ssh_key
    log_info "腾讯云服务器服务状态:"
    ve_ssh "
        cd ${SERVER_DEPLOY_DIR} 2>/dev/null && docker compose ps 2>/dev/null || echo '未部署'
        echo '---'
        echo '内存使用:'
        free -h
        echo '---'
        echo '容器资源:'
        docker stats --no-stream --format 'table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}' 2>/dev/null || echo '无运行中容器'
    "
}

# ---- 查看日志 ----
do_logs() {
    setup_ssh_key
    local service="${2:-backend}"
    log_info "查看 ${service} 日志 (最近50行):"
    ve_ssh "cd ${SERVER_DEPLOY_DIR} && docker compose logs --tail=50 ${service}"
}

# ---- 回滚 ----
do_rollback() {
    setup_ssh_key
    log_warn "回滚到上一版本..."
    ve_ssh "
        cd ${SERVER_DEPLOY_DIR}
        docker compose down
        # 使用上一次拉取的镜像重启
        docker compose up -d
    "
    log_ok "回滚完成"
}

# ---- 健康检查 ----
do_healthcheck() {
    setup_ssh_key
    log_info "健康检查..."
    ve_ssh "
        cd ${SERVER_DEPLOY_DIR} >/dev/null 2>&1 || true
        echo '>>> 后端健康检查:'
        curl -sf http://localhost:8080/api/health 2>/dev/null && echo ' ✓ 后端正常' || echo ' ✗ 后端异常'
        echo '>>> 前端检查:'
        curl -sf -o /dev/null http://localhost:3000 && echo ' ✓ 前端正常' || echo ' ✗ 前端异常'
    "
}

# ---- 主入口 ----
case "${1:-deploy}" in
    build|push|sync-base|all|"")
        deny_local_build
        ;;
    deploy)
        do_deploy
        ;;
    status)
        do_status
        ;;
    logs)
        do_logs "$@"
        ;;
    rollback)
        do_rollback
        ;;
    health|healthcheck)
        do_healthcheck
        ;;
    *)
        echo "用法: IMAGE_TAG=时间戳 $0 {deploy|status|logs [service]|rollback|healthcheck}"
        exit 1
        ;;
esac
