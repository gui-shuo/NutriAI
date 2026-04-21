# Findings & Decisions

## Requirements
- 不再使用 GitHub 自动化部署到服务器。
- 镜像构建改由阿里云容器镜像服务自动从 GitHub 仓库构建。
- 项目构建需要的基础镜像都切到阿里云公共镜像。
- 需要明确阿里云构建出的镜像命名方式，尤其是时间戳标签。
- 需要服务器侧自动化脚本：拉取最新时间戳镜像并直接运行容器。

## Follow-up Requirements
- 自动化流程改为 Gitee Go 工作流，不再依赖 GitHub Actions / GitHub 仓库工作流。
- 镜像存储位置改为腾讯云 TCR。
- 其他约束保持不变：继续使用时间戳 tag、继续使用 docker compose 服务器部署，但不再依赖手工 deploy.sh。

## Follow-up Findings
- 当前仓库已经没有 GitHub deploy workflow，但 README、docker-compose.prod.yml、.env.example 和 docs 在切换前仍全面使用 ACR 语义。
- Gitee Go 官方文档确认 YAML 顶层字段为 version / name / displayName / triggers / variables / stages。
- Gitee Go 官方文档确认可用 deploy@agent 与 shell@agent 主机组插件执行远端脚本。
- Gitee Go 的 build@docker 插件文档未暴露独立 build context 配置，和当前 backend/frontend 子目录 Docker 构建方式存在适配风险。
- 腾讯云 TCR 文档确认个人版登录地址为 ccr.ccs.tencentyun.com，仓库命名格式为 ccr.ccs.tencentyun.com/<namespace>/<repo>:<tag>。
- 最终实现采用 Gitee Go 的官方主机组部署能力，而不是单独 SSH 插件；在目标腾讯云服务器上执行的命令与 SSH 远端执行等价。
- 为兼容 Gitee Go 的根目录 Docker 构建上下文，新增了两份根目录包装 Dockerfile，避免破坏现有 backend/frontend 本地 Dockerfile。
- Gitee Go 的手动执行不需要额外 YAML 字段；去掉 `triggers` 后，流水线仍可在页面中手动点击执行。
- Gitee Go 官方示例与帮助文档中的流水线 YAML 存放位置是仓库根目录下的 `.workflow/` 目录；将 YAML 放在仓库根目录可能不会被平台识别为流水线。
- 本轮按用户给定的 5 篇官方文档重新核对后，确认当前活动文件路径是 `.gitee/pipeline-docker.yml`，需基于现有仓库状态修正 YAML，而不是继续假设 `.workflow` 为唯一生效路径。
- `basic-config` 文档确认顶层最少包含 `version`、`name`、`displayName`，其中 `name` 只能使用小写字母、数字、中划线、下划线。
- `trigger` 文档确认只有配置 `triggers` 才会自动触发；手动执行模式可以完全省略 `triggers`。
- `scheduling` 文档确认 `stages` 的官方写法是 `- stage:` 后，与 `name`、`displayName`、`strategy`、`trigger`、`steps` 同级缩进，不应再把这些字段嵌套到 `stage` 下一级。
- `parameter` 文档确认用户自定义流水线参数放在 `variables`，运行中动态更新参数使用 `echo 'KEY=VALUE' >> GITEE_PARAMS`，并且 `GITEE_`、`GO_` 前缀是保留字。
- `advantage-options` 文档确认可以在顶层 `strategy` 中配置 `blocking` 和 `stepTimeout`；对生产部署流水线，开启阻塞构建可以避免多个部署互相覆盖。
- `image-build-and-deployment` 插件文档确认 `build@docker` 的核心字段只有 `repository`、`username`、`password`、`tag`、`dockerfile`、`artifacts`、`isCache`，其中 `tag` 的官方例子是 `镜像名:版本` 形式。
- 当前 `build_backend_image` / `build_frontend_image` 任务最可疑的配置点有两个：一是 YAML 中引用了 `TCR_NAMESPACE`、`TCR_USERNAME`、`TCR_PASSWORD`、`DEPLOY_HOST_GROUP_ID` 却没有在活动流水线文件里声明；二是把 `namespace/repo:tag` 全塞进 `tag` 字段，和官方示例的 `image:tag` 形态不一致。
- 为降低 Gitee Go 代码视图的参数校验失败概率，活动流水线改为：在 `variables` 中显式声明这些参数占位值，并把 `repository` 调整为 `${TCR_REGISTRY}/${TCR_NAMESPACE}`、`tag` 调整为 `${BACKEND_IMAGE_REPO}:${TIMESTAMP_TAG}` / `${FRONTEND_IMAGE_REPO}:${TIMESTAMP_TAG}`。
- 用户最新从 Gitee Go 页面复制出的 YAML 说明：该仓库当前生效的是 `.gitee/pipeline-docker.yml`，而且图形视图保存后会改写结构，例如 `stages` 变成直接对象数组、`script` 变成字符串数组、`hostGroupID` 变成对象。
- 对于 `deploy@agent`，官方文档明确 `deployArtifact[].name` 是下载到主机后的部署包名，默认 `output`。因此它不能继续使用 `nutriai-deploy-bundle.tar.gz` 这类带点号的文件名，需改为纯数字/字母/中划线/下划线形式，并同步修改脚本中的目标文件路径。
- “仓库的认证方式没有配置” 这一项在官方 `build@docker` 文档里没有对应公开 YAML 字段说明；用户导出的 YAML 中出现了 `type: account`，因此更稳妥的处理方式是保留导出结构，通过 Gitee Go 图形视图重新选择一次仓库认证方式，而不是继续猜测代码字段。
- Gitee Go 当前还要求至少配置一个事件监听，因此不能继续使用 `triggers: trigger: manual` 这种非官方写法。为了兼顾“必须有监听器”和“尽量不要普通 push 自动部署”，活动流水线改为只监听精确 Tag `manual-deploy`。
- 本次构建失败的直接根因来自 Dockerfile 第一行 `# syntax=docker/dockerfile:1.7`。Gitee Go 的 buildkit 在解析这行时会访问 `docker.io/docker/dockerfile:1.7`，而当前网络到 Docker Hub 超时，导致还没开始拉基础镜像就直接失败。
- 两份 Gitee 专用包装 Dockerfile 和原始 backend/frontend Dockerfile 都使用了 `RUN --mount=type=cache`，这会继续依赖 syntax frontend，因此一起改为普通 `RUN` 更稳，代价只是构建缓存优化消失。
- 用户最新导出的 `.gitee/pipeline-docker.yml` 丢失了 `TCR_REGISTRY`、`TCR_NAMESPACE`、`TCR_USERNAME`、`TCR_PASSWORD` 四个变量，但部署脚本仍然依赖它们；此外镜像构建 tag 不含命名空间，后续推送到 TCR 大概率也会失败，因此需要一并补回。

## Research Findings
- backend/Dockerfile 默认基础镜像仍为 maven:3.9-eclipse-temurin-17 与 eclipse-temurin:17-jre-alpine。
- frontend/Dockerfile 默认基础镜像仍为 node:20-alpine 与 nginx:1.25-alpine。
- docker-compose.prod.yml 仍使用 TCR_REGISTRY/TCR_NAMESPACE 变量命名，默认值仍是旧腾讯云仓库。
- 旧的 .github/workflows/deploy.yml 已移除，仓库内不再保留 GitHub 远端部署流程。
- 阿里云 ACR 个人版官方文档确认：源码自动构建支持公开公网基础镜像、同地域同账号私有公网镜像，不支持第三方授权镜像或跨地域私有镜像。
- 阿里云 ACR 个人版官方文档在构建规则中提供“镜像版本”字段，但公开文档没有说明支持 DATETIME 之类的动态变量模板。
- 阿里云云效流水线文档明确支持使用 DATETIME 作为镜像标签，但这属于云效流水线能力，不等同于 ACR 个人版源码自动构建能力。
- 阿里云公开资料可以确认 registry.cn-hangzhou.aliyuncs.com/acs/maven、registry.cn-hangzhou.aliyuncs.com/acs/nginx、registry.cn-hangzhou.aliyuncs.com/acs/node、registry.cn-hangzhou.aliyuncs.com/acs/openjdk 这类公共镜像前缀存在，但具体 tag 兼容性需要保留可覆盖能力。

## Technical Decisions
| Decision | Rationale |
|----------|-----------|
| 服务器自动部署脚本继续基于 docker compose | 项目本身就是多容器，直接 docker run 需要手动管理网络、卷和健康依赖，复杂度高 |
| 基础镜像默认值改为阿里云公共镜像，并保留 build-arg 覆盖能力 | 阿里云公共镜像是目标默认源，但公开资料对部分 tag 支持不够稳定，保留覆盖能力更稳 |
| 自动部署脚本按“后端和前端公共时间戳 tag 交集”选择最新版本 | 能避免前后端分别取各自最新 tag 导致版本不一致 |
| README 与 .env.example 同步切换到 ACR 命名 | 避免运行脚本、compose、文档三处继续混用旧的 TCR 变量 |

## Issues Encountered
| Issue | Resolution |
|-------|------------|
| 阿里云公共镜像的具体 tag 文档不完整 | 采用已确认存在的公共镜像仓库前缀，并把具体镜像名保留为可覆盖默认值 |
| ACR 个人版是否支持动态时间戳镜像 tag 未见官方说明 | 在交付说明中明确：时间戳 tag 规则需在 ACR 控制台确认；如不能动态生成，则建议改为手动 git tag 触发或使用云效流水线 |

## Resources
- backend/Dockerfile
- frontend/Dockerfile
- docker-compose.prod.yml
- .github/workflows/deploy.yml
- https://help.aliyun.com/zh/acr/user-guide/create-a-repository-and-build-images
- https://help.aliyun.com/zh/yunxiao/use-cases/java-application-build-and-deploy-k8s
- https://github.com/AliyunContainerService/maven-image

## Visual/Browser Findings
- 暂无
