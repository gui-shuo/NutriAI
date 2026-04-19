# syntax=docker/dockerfile:1.7

# 多阶段构建
ARG MAVEN_BUILDER_IMAGE=ccr.ccs.tencentyun.com/heyitshuo/base-maven:3.9-eclipse-temurin-17
ARG JAVA_RUNTIME_IMAGE=ccr.ccs.tencentyun.com/heyitshuo/base-eclipse-temurin:17-jre-alpine

# 阶段1：构建
FROM ${MAVEN_BUILDER_IMAGE} AS builder

WORKDIR /app

ARG HTTP_PROXY=
ARG HTTPS_PROXY=
ARG ALL_PROXY=
ARG NO_PROXY=
ARG MAVEN_MIRROR_URL=https://maven.aliyun.com/repository/public
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    ALL_PROXY=${ALL_PROXY} \
    NO_PROXY=${NO_PROXY} \
    http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    all_proxy=${ALL_PROXY} \
    no_proxy=${NO_PROXY} \
    MAVEN_OPTS="-Dmaven.repo.local=/root/.m2/repository"


RUN mkdir -p /root/.m2 && \
    printf '%s\n' \
    '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' \
    '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' \
    '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">' \
    '  <mirrors>' \
    '    <mirror>' \
    '      <id>aliyun-public</id>' \
    '      <mirrorOf>*</mirrorOf>' \
    '      <name>Aliyun Maven</name>' \
    "      <url>${MAVEN_MIRROR_URL}</url>" \
    '    </mirror>' \
    '  </mirrors>' \
    '</settings>' > /root/.m2/settings.xml

# 复制 pom.xml 并预热依赖缓存
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s /root/.m2/settings.xml -B dependency:go-offline

# 复制源代码并构建
COPY src ./src
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -s /root/.m2/settings.xml -B clean package -DskipTests -Pprod

# 阶段2：运行
FROM ${JAVA_RUNTIME_IMAGE}

WORKDIR /app

ARG HTTP_PROXY=
ARG HTTPS_PROXY=
ARG ALL_PROXY=
ARG NO_PROXY=
ENV HTTP_PROXY=${HTTP_PROXY} \
    HTTPS_PROXY=${HTTPS_PROXY} \
    ALL_PROXY=${ALL_PROXY} \
    NO_PROXY=${NO_PROXY} \
    http_proxy=${HTTP_PROXY} \
    https_proxy=${HTTPS_PROXY} \
    all_proxy=${ALL_PROXY} \
    no_proxy=${NO_PROXY}

# 安装必要的工具和中文字体
RUN apk add --no-cache tzdata font-noto-cjk wget && \

    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone

# 从构建阶段复制 JAR 包
COPY --from=builder /app/target/*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

