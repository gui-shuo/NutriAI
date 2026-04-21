# 阿里云 ACR 自动构建配置说明

## 目标

本项目约定使用阿里云 ACR 个人版绑定 GitHub 仓库自动构建两个应用镜像：

- nutriai-backend
- nutriai-frontend

服务器侧部署脚本 deploy.sh 会从这两个仓库中找出共同存在的最新时间戳 tag，然后使用 docker compose 启动容器。

## 镜像命名约定

- 仓库地址：`<命名空间>/nutriai-backend`
- 仓库地址：`<命名空间>/nutriai-frontend`
- 镜像 tag：推荐使用 14 位时间戳，格式为 `YYYYMMDDHHMMSS`
- 生产部署只消费“后端与前端都存在”的公共 tag

## 构建仓库建议

### 后端仓库

- 镜像仓库名称：nutriai-backend
- 代码源：当前 GitHub 仓库
- 构建上下文目录：backend
- Dockerfile 文件名：Dockerfile
- 自动构建：开启
- 海外机器构建：按需开启；如果基础依赖主要在国内，建议关闭
- 不使用缓存：关闭

### 前端仓库

- 镜像仓库名称：nutriai-frontend
- 代码源：当前 GitHub 仓库
- 构建上下文目录：frontend
- Dockerfile 文件名：Dockerfile
- 自动构建：开启
- 海外机器构建：按需开启；如果基础依赖主要在国内，建议关闭
- 不使用缓存：关闭

## 基础镜像来源

仓库内 Dockerfile 默认已经切到阿里云公共镜像前缀，并保留 build-arg 覆盖能力：

- backend builder：registry.cn-hangzhou.aliyuncs.com/acs/maven:3.9-eclipse-temurin-17
- backend runtime：registry.cn-hangzhou.aliyuncs.com/acs/openjdk:17-jre-alpine
- frontend builder：registry.cn-hangzhou.aliyuncs.com/acs/node:20-alpine
- frontend runtime：registry.cn-hangzhou.aliyuncs.com/acs/nginx:1.25-alpine
- redis runtime：registry.cn-hangzhou.aliyuncs.com/acs/redis:6.0-alpine

如果你所在地域或实例中某个 tag 无法拉取，可在 ACR 构建规则里通过构建参数覆盖对应基础镜像。

## 关于时间戳 tag

阿里云 ACR 个人版公开文档明确提供“镜像版本”字段，但未公开说明支持 DATETIME 这类动态变量模板。当前推荐按下面优先级处理：

1. 如果你的 ACR 控制台支持动态时间戳镜像版本，直接将后端与前端仓库都配置为 `YYYYMMDDHHMMSS` 风格 tag。
2. 如果个人版控制台不支持动态时间戳 tag，但你仍要严格使用时间戳版本：
   - 方案 A：改为使用 Git tag 触发构建，并让 tag 名本身就是时间戳。
   - 方案 B：改用云效流水线，镜像标签可使用 `${DATETIME}`。
3. 如果暂时只能生成 `latest`：
   - deploy.sh 默认不会自动部署 `latest`，因为它会优先要求时间戳 tag。
   - 如需临时放行，可在服务器执行前设置 `ALLOW_LATEST_FALLBACK=true`。

## 服务器部署脚本依赖

deploy.sh 运行在服务器本地，依赖以下命令：

- docker
- docker compose 或 docker-compose
- curl
- python3

脚本会调用 Docker Registry V2 接口查询 tags：

- `https://<ACR_REGISTRY>/v2/<ACR_NAMESPACE>/nutriai-backend/tags/list`
- `https://<ACR_REGISTRY>/v2/<ACR_NAMESPACE>/nutriai-frontend/tags/list`

因此服务器 `.env` 中必须配置：

- ACR_REGISTRY
- ACR_NAMESPACE
- ACR_USERNAME
- ACR_PASSWORD

## 推荐部署动作

```bash
cd /www/wwwroot/nutriai
./deploy.sh resolve-tag
./deploy.sh deploy
```

如果需要回滚到指定版本：

```bash
cd /www/wwwroot/nutriai
./deploy.sh deploy 20260421123045
```