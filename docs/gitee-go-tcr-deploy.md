# Gitee Go + 腾讯云 TCR 自动部署说明

## 目标

本项目使用 Gitee Go 工作流完成以下全流程：

- 生成 14 位时间戳镜像标签
- 构建 `nutriai-backend` 与 `nutriai-frontend` 镜像
- 推送到腾讯云 TCR
- 在目标腾讯云服务器上拉取本次镜像并重建容器
- 清理旧容器与旧镜像

## 工作流文件

仓库新增了 `.workflow/pipeline-docker.yml`，用于 Gitee Go 的代码视图流水线配置。

同时新增了两份包装 Dockerfile：

- `Dockerfile.backend.gitee`
- `Dockerfile.frontend.gitee`

原因：Gitee Go 的 `build@docker` 插件默认使用仓库根目录作为构建上下文，未公开独立的 build context 字段。包装 Dockerfile 负责在“根目录上下文”下继续构建 backend/frontend 子项目，而不破坏原有本地 Dockerfile 语义。

## 腾讯云 TCR 命名约定

- 镜像仓库地址：`ccr.ccs.tencentyun.com`
- 后端镜像：`ccr.ccs.tencentyun.com/<TCR_NAMESPACE>/nutriai-backend:<TIMESTAMP_TAG>`
- 前端镜像：`ccr.ccs.tencentyun.com/<TCR_NAMESPACE>/nutriai-frontend:<TIMESTAMP_TAG>`
- 镜像 tag：`YYYYMMDDHHMMSS`

## Gitee Go 必填参数

建议在 Gitee Go 的全局参数或流水线参数中配置以下值：

- `TCR_NAMESPACE`
- `TCR_USERNAME`
- `TCR_PASSWORD`
- `DEPLOY_HOST_GROUP_ID`

仓库内 `.workflow/pipeline-docker.yml` 已内置以下默认参数：

- `TCR_REGISTRY=ccr.ccs.tencentyun.com`
- `BACKEND_IMAGE_REPO=nutriai-backend`
- `FRONTEND_IMAGE_REPO=nutriai-frontend`
- `REDIS_IMAGE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:6.0-alpine`
- `DEPLOY_DIR=/www/wwwroot/nutriai`

## 目标腾讯云服务器要求

目标服务器需满足：

- 已安装 Docker
- 已安装 `docker compose` 或 `docker-compose`
- 已加入 Gitee Go 主机组
- `DEPLOY_DIR` 下已存在 `.env`

Gitee Go 官方文档对远端部署公开的是“主机组 Agent”方式，而不是独立 SSH 插件。当前实现采用主机组 Agent 在目标腾讯云服务器上执行部署命令，效果等价于工作流远端登录该主机执行脚本。

## 服务器 .env 最少需要的镜像相关字段

```ini
TCR_REGISTRY=ccr.ccs.tencentyun.com
TCR_NAMESPACE=your_tcr_namespace
BACKEND_IMAGE_REPO=nutriai-backend
FRONTEND_IMAGE_REPO=nutriai-frontend
REDIS_IMAGE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:6.0-alpine
IMAGE_TAG=20260101000000
```

工作流部署阶段会自动把 `IMAGE_TAG` 更新为本次构建的时间戳值，并同步更新上述镜像仓库字段。

## 部署阶段执行逻辑

工作流部署阶段在腾讯云服务器上执行以下动作：

1. 下发最新的 `docker-compose.prod.yml`
2. 更新 `.env` 中的 `IMAGE_TAG`、`TCR_REGISTRY`、`TCR_NAMESPACE`、镜像仓库名
3. 登录腾讯云 TCR
4. 执行 `docker compose pull redis backend frontend`
5. 执行 `docker compose up -d --force-recreate --remove-orphans`
6. 仅删除 `nutriai` 项目的旧停止容器
7. 删除不是当前时间戳 tag 的旧后端/前端镜像
8. 执行 `docker image prune -f`

## 容器资源约束

容器最大内存与 swap 等限制继续由 `docker-compose.prod.yml` 控制，当前主要配置为：

- `redis`: `mem_limit: 128m`
- `backend`: `mem_limit: 512m`
- `frontend`: `mem_limit: 64m`

如果需要调整腾讯云服务器上的容器资源上限，请修改 `docker-compose.prod.yml` 并重新提交到 Gitee，由下一次工作流自动下发。

## 触发方式

当前 `.workflow/pipeline-docker.yml` 不配置自动触发器，默认采用手动执行模式：

- 在 Gitee Go 页面点击“执行流水线”
- 执行时选择目标分支，生产建议选择 `master`

每次手动执行都会重新生成时间戳 tag，并触发完整的构建、推送、部署、清理流程。

如果 Gitee Go 页面显示“未识别到有流水线”，优先检查以下两项：

1. 仓库是否已经开通 Gitee Go。
2. 默认分支中是否已经提交 `.workflow/pipeline-docker.yml`。

根据 Gitee Go 官方帮助文档，平台创建流水线时会在仓库根目录生成 `.workflow` 文件夹并在其中保存 YAML 描述文件。