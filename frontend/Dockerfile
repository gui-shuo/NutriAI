FROM node:20-alpine AS builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY . .

# 生产环境使用相对路径，通过 nginx 代理转发到后端
ARG VITE_API_BASE_URL=/api
ARG VITE_WS_BASE_URL=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_WS_BASE_URL=${VITE_WS_BASE_URL}

RUN npm run build

FROM nginx:1.25-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

# H5 (UniApp) 构建产物由外部预构建后放入 h5-dist/
# 如果目录不存在则跳过（兼容无 H5 的场景）
COPY h5-dis[t] /usr/share/nginx/h5

EXPOSE 80
