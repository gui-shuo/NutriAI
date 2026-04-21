# syntax=docker/dockerfile:1.7

# GitHub Actions 构建时默认直接从 Docker 官方镜像拉取基础镜像
ARG NODE_BUILDER_IMAGE=node:20-alpine
ARG NGINX_RUNTIME_IMAGE=nginx:1.25-alpine

FROM ${NODE_BUILDER_IMAGE} AS builder

WORKDIR /app

ARG HTTP_PROXY=
ARG HTTPS_PROXY=
ARG ALL_PROXY=
ARG NO_PROXY=
ARG NPM_REGISTRY=https://registry.npmjs.org
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    ALL_PROXY=${ALL_PROXY} \
    NO_PROXY=${NO_PROXY} \
    http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    all_proxy=${ALL_PROXY} \
    no_proxy=${NO_PROXY} \
    npm_config_registry=${NPM_REGISTRY} \
    npm_config_cache=/root/.npm


COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm \
    npm ci --prefer-offline --no-audit

COPY . .

# 生产环境使用相对路径，通过 nginx 代理转发到后端
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_BASE_URL=${VITE_WS_BASE_URL}

RUN --mount=type=cache,target=/root/.npm \
    npm run build

FROM ${NGINX_RUNTIME_IMAGE}

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

