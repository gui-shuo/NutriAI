#!/bin/bash
#===============================================================================
# NutriAI 部署脚本
# 用法:
#   ./deploy.sh              # 构建+推送+部署 (全流程)
#   ./deploy.sh build        # 仅构建镜像
#   ./deploy.sh push         # 仅推送到 Docker Hub
#   ./deploy.sh deploy       # 仅部署到火山引擎 (拉取+启动)
#   ./deploy.sh status       # 查看火山引擎服务状态
#   ./deploy.sh logs         # 查看火山引擎容器日志
#   ./deploy.sh rollback     # 回滚到上一个版本
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
        if [[ "$line" == VOLCENGINE_SSH_KEY=* ]]; then
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

# ---- 配置 ----
DOCKER_USER="${DOCKER_NAME:-heyitshuo}"
BACKEND_IMAGE="${DOCKER_USER}/nutriai-backend"
FRONTEND_IMAGE="${DOCKER_USER}/nutriai-frontend"
VE_HOST="${VOLCENGINE_IPV4}"
VE_USER="${VOLCENGINE_SSH_USER:-root}"
VE_DEPLOY_DIR="/www/wwwroot/nutriai"
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

# ---- 提取 SSH Key ----
setup_ssh_key() {
    SSH_KEY_FILE=$(mktemp /tmp/ve_ssh_XXXXXX)
    python3 -c "
with open('${PROJECT_DIR}/.env') as f:
    content = f.read()
idx = content.index('VOLCENGINE_SSH_KEY=')
key_part = content[idx + len('VOLCENGINE_SSH_KEY='):]
# Strip surrounding quotes
key_part = key_part.lstrip('\"').lstrip(\"'\")
end_idx = key_part.index('-----END OPENSSH PRIVATE KEY-----') + len('-----END OPENSSH PRIVATE KEY-----')
key = key_part[:end_idx].strip()
with open('${SSH_KEY_FILE}', 'w') as f:
    f.write(key + '\n')
import os; os.chmod('${SSH_KEY_FILE}', 0o600)
"
}

cleanup_ssh_key() {
    [ -n "$SSH_KEY_FILE" ] && rm -f "$SSH_KEY_FILE"
}
trap cleanup_ssh_key EXIT

# ---- SSH 封装 ----
ve_ssh() {
    ssh -o StrictHostKeyChecking=no -o ConnectTimeout=15 -i "$SSH_KEY_FILE" "${VE_USER}@${VE_HOST}" "$@"
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

# ---- 生成时间戳标签 ----
generate_tag() {
    echo "v$(date +%Y%m%d%H%M)"
}

# ---- 更新 docker-compose 文件中的镜像标签 ----
update_compose_tags() {
    local tag="$1"
    log_info "更新 docker-compose 文件中的镜像标签为 ${tag}..."
    for f in docker-compose.local.yml docker-compose.volcengine.yml; do
        if [ -f "$f" ]; then
            sed -i "s|${BACKEND_IMAGE}:[^ ]*|${BACKEND_IMAGE}:${tag}|g" "$f"
            sed -i "s|${FRONTEND_IMAGE}:[^ ]*|${FRONTEND_IMAGE}:${tag}|g" "$f"
            log_ok "已更新 $f"
        fi
    done
}

# ---- 构建镜像 ----
do_build() {
    log_info "========== 构建 Docker 镜像 =========="

    IMAGE_TAG=$(generate_tag)
    log_info "镜像标签: ${IMAGE_TAG}"

    log_info "构建后端镜像..."
    docker build -t "${BACKEND_IMAGE}:${IMAGE_TAG}" ./backend
    log_ok "后端镜像构建完成"

    log_info "构建前端镜像..."
    docker build -t "${FRONTEND_IMAGE}:${IMAGE_TAG}" ./frontend
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
    if [ -f /tmp/nutriai_image_tag ]; then
        cat /tmp/nutriai_image_tag
    else
        # 从 docker-compose.volcengine.yml 提取当前标签
        grep -oP "${BACKEND_IMAGE}:\K[^ ]+" docker-compose.volcengine.yml 2>/dev/null || echo "unknown"
    fi
}

# ---- 推送镜像 ----
do_push() {
    log_info "========== 推送镜像到 Docker Hub =========="
    docker_login

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

# ---- 部署到火山引擎 ----
do_deploy() {
    log_info "========== 部署到火山引擎 =========="
    setup_ssh_key

    IMAGE_TAG=$(get_current_tag)
    log_info "部署标签: ${IMAGE_TAG}"

    # 创建部署目录
    ve_ssh "mkdir -p ${VE_DEPLOY_DIR}/releases"

    # 上传 docker-compose 文件
    log_info "上传 docker-compose 配置..."
    ve_scp "${PROJECT_DIR}/docker-compose.volcengine.yml" "${VE_USER}@${VE_HOST}:${VE_DEPLOY_DIR}/docker-compose.yml"

    # 生成并上传 .env (排除 SSH Key 和 Docker 凭据等本地变量)
    log_info "同步环境变量..."
    python3 -c "
with open('${PROJECT_DIR}/.env') as f:
    content = f.read()
lines = []
skip = False
skip_keys = {'VOLCENGINE_SSH_KEY', 'VOLCENGINE_IPV4', 'VOLCENGINE_SSH_USER', 'DOCKER_NAME', 'DOCKER_KEY'}
for line in content.split('\n'):
    if skip:
        if 'END OPENSSH PRIVATE KEY' in line:
            skip = False
        continue
    key = line.split('=')[0].strip() if '=' in line else ''
    if key in skip_keys:
        if 'SSH_KEY' in key:
            skip = True
        continue
    lines.append(line)
with open('/tmp/ve_env', 'w') as f:
    f.write('\n'.join(lines))
"
    ve_scp "/tmp/ve_env" "${VE_USER}@${VE_HOST}:${VE_DEPLOY_DIR}/.env"
    rm -f /tmp/ve_env
    log_ok "环境变量同步完成"

    # 拉取镜像并启动
    log_info "在火山引擎服务器上拉取镜像并启动..."
    ve_ssh "
        cd ${VE_DEPLOY_DIR}

        echo '>>> 拉取镜像 (标签: ${IMAGE_TAG})...'
        for i in 1 2 3; do
            docker pull ${BACKEND_IMAGE}:${IMAGE_TAG} && break
            echo \"拉取后端镜像失败，第\${i}次重试...\"
            sleep 5
        done
        for i in 1 2 3; do
            docker pull ${FRONTEND_IMAGE}:${IMAGE_TAG} && break
            echo \"拉取前端镜像失败，第\${i}次重试...\"
            sleep 5
        done

        echo '>>> 停止旧容器...'
        docker compose down --remove-orphans 2>/dev/null || true

        echo '>>> 启动新容器...'
        docker compose up -d

        echo '>>> 清理悬空镜像...'
        docker image prune -f 2>/dev/null || true

        echo '>>> 等待服务启动...'
        sleep 10

        echo '>>> 容器状态:'
        docker compose ps
    "

    log_ok "部署完成！(标签: ${IMAGE_TAG})"
}

# ---- 查看状态 ----
do_status() {
    setup_ssh_key
    log_info "火山引擎服务状态:"
    ve_ssh "
        cd ${VE_DEPLOY_DIR} 2>/dev/null && docker compose ps 2>/dev/null || echo '未部署'
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
    ve_ssh "cd ${VE_DEPLOY_DIR} && docker compose logs --tail=50 ${service}"
}

# ---- 回滚 ----
do_rollback() {
    setup_ssh_key
    log_warn "回滚到上一版本..."
    ve_ssh "
        cd ${VE_DEPLOY_DIR}
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
        echo '>>> 后端健康检查:'
        curl -sf http://localhost:8080/api/health 2>/dev/null && echo ' ✓ 后端正常' || echo ' ✗ 后端异常'
        echo '>>> 前端检查:'
        curl -sf -o /dev/null http://localhost:3000 && echo ' ✓ 前端正常' || echo ' ✗ 前端异常'
    "
}

# ---- 主入口 ----
case "${1:-all}" in
    build)
        do_build
        ;;
    push)
        do_push
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
        setup_ssh_key
        do_healthcheck
        ;;
    all|"")
        do_build
        do_push
        do_deploy
        echo ""
        log_info "等待 30 秒后进行健康检查..."
        sleep 30
        do_healthcheck
        echo ""
        log_ok "========== 全流程部署完成 =========="
        ;;
    *)
        echo "用法: $0 {build|push|deploy|status|logs [service]|rollback|healthcheck|all}"
        exit 1
        ;;
esac
