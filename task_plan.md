# Task Plan: Aliyun Image Build And Runtime Migration

## Goal
将项目的镜像构建与运行链路改为阿里云容器镜像服务：基础镜像改用阿里云公共镜像，明确阿里云自动构建使用的镜像命名规则，并提供服务器侧自动拉取最新时间戳镜像并启动容器的脚本。

## Current Phase
Phase 5

## Phases
### Phase 1: Requirements & Discovery
- [x] Understand user intent
- [x] Identify constraints and requirements
- [x] Document findings in findings.md
- **Status:** complete

### Phase 2: Planning & Structure
- [x] Define technical approach
- [x] Decide image naming convention for Aliyun auto-build
- [x] Decide runtime deployment flow on server
- **Status:** complete

### Phase 3: Implementation
- [x] Update Dockerfiles to use Aliyun public base images
- [x] Update compose/env/scripts for Aliyun registry naming
- [x] Remove or neutralize obsolete GitHub deployment path
- [x] Add automated server-side pull-and-run script
- **Status:** complete

### Phase 4: Testing & Verification
- [x] Validate edited files for syntax/config issues
- [x] Verify deployment script logic and assumptions
- [x] Document any manual prerequisites
- **Status:** complete

### Phase 5: Delivery
- [x] Summarize changes and remaining setup steps
- [x] Call out anything that must be configured in Aliyun console
- [x] Deliver final usage instructions
- **Status:** complete

## Key Questions
1. 阿里云自动构建的时间戳标签规则是否能完全在仓库文件中表达，还是需要在 ACR 控制台中配置？
2. 服务器启动容器应继续使用 docker compose，还是直接 docker run 多个容器？
3. 当前 GitHub deploy workflow 是否应移除，还是保留但停止使用？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 运行侧优先继续使用 docker compose | 项目包含 redis、backend、frontend，多容器编排继续用 compose 更稳，脚本只负责解析最新 tag 并启动 |
| 废弃 GitHub deploy workflow | 用户已明确不再使用 GitHub 自动化部署，保留旧 workflow 只会继续误导运维路径 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       | 1       |            |

## Notes
- 需要避免把用户提供的阿里云密码写入仓库文件。
- 如果阿里云构建命名规则无法由仓库控制，需要在最终说明里明确哪些项需在 ACR 控制台设置。
- bash 在当前 Windows 工作区不可用，因此 deploy.sh 未做本机 bash -n 校验，只做了静态问题检查。
