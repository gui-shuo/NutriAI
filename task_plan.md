# Task Plan: Deployment Automation Migration

## Goal
当前目标是在保留“时间戳镜像 + docker compose 服务器部署”约束的前提下，将自动化链路从 GitHub / 阿里云 ACR 改为 Gitee Go / 腾讯云 TCR。

## Current Phase
Phase 10

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

### Phase 6: Gitee Go Discovery
- [x] Confirm Gitee Go YAML structure and plugin capabilities
- [x] Confirm TCR login and image naming assumptions
- [x] Decide whether to build on Gitee cloud or host group
- **Status:** complete

### Phase 7: Gitee Go Implementation
- [x] Add Gitee Go pipeline YAML
- [x] Switch deploy script and compose/env naming from ACR to TCR
- [x] Add any Gitee-specific Docker/build wrappers if needed
- **Status:** complete

### Phase 8: Docs Refresh
- [x] Rewrite README deployment section for Gitee Go + TCR
- [x] Replace Aliyun-specific build doc with Gitee/TCR instructions
- [x] Document required Gitee Go global parameters and host groups
- **Status:** complete

### Phase 9: Verification
- [x] Validate edited files for syntax/config issues
- [x] Check for stale ACR/GitHub references in deployment path
- [x] Note any manual prerequisites that cannot be verified locally
- **Status:** complete

### Phase 10: Manual Trigger Adjustment
- [x] Remove automatic pipeline triggers
- [x] Rewrite docs for manual execution flow
- [x] Re-verify deployment path after trigger change
- **Status:** complete

## Key Questions
1. 阿里云自动构建的时间戳标签规则是否能完全在仓库文件中表达，还是需要在 ACR 控制台中配置？
2. 服务器启动容器应继续使用 docker compose，还是直接 docker run 多个容器？
3. 当前 GitHub deploy workflow 是否应移除，还是保留但停止使用？
4. Gitee Go 是否适合直接用 build@docker，还是应该改为主机组脚本构建以兼容当前子目录 Docker context？

## Decisions Made
| Decision | Rationale |
|----------|-----------|
| 运行侧优先继续使用 docker compose | 项目包含 redis、backend、frontend，多容器编排继续用 compose 更稳，脚本只负责解析最新 tag 并启动 |
| 废弃 GitHub deploy workflow | 用户已明确不再使用 GitHub 自动化部署，保留旧 workflow 只会继续误导运维路径 |
| 优先保留当前 backend/frontend 子目录构建方式 | 避免为适配新工作流而破坏现有 Dockerfile 的本地构建语义 |

## Errors Encountered
| Error | Attempt | Resolution |
|-------|---------|------------|
|       | 1       |            |

## Notes
- 需要避免把用户提供的阿里云密码写入仓库文件。
- 如果阿里云构建命名规则无法由仓库控制，需要在最终说明里明确哪些项需在 ACR 控制台设置。
- bash 在当前 Windows 工作区不可用，因此 deploy.sh 未做本机 bash -n 校验，只做了静态问题检查。
- 新任务要求改用 Gitee Go 与腾讯云 TCR，需刷新上一轮的 README / docs / deploy.sh / compose 语义。
