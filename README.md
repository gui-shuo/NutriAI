# AI 健康饮食规划助手系统 (NutriAI)

基于 Vue 3 + Spring Boot + 通义千问大模型的智能健康管理平台。通过 AI 将专业营养学知识转化为可执行的日常饮食计划，解决"吃得健康却不知如何搭配"的核心痛点。

---

## 核心特性

- **AI 智能规划** — 基于通义千问 API，3 分钟生成个性化饮食方案
- **实时对话** — WebSocket 流式输出 AI 回复，支持多轮上下文
- **会员成长体系** — 三级会员（普通/白银/黄金），游戏化激励健康习惯
- **营养数据库** — 3000+ 食材营养数据，精准计算每日摄入
- **数据可视化** — ECharts 图表展示营养趋势和成长曲线
- **食物识别** — 拍照上传，AI 自动识别食材与营养成分
- **后台管理** — 数据看板、用户管理、AI 调用日志、系统配置
- **响应式设计** — 适配 Mobile / Tablet / Desktop

---

## 技术栈

| 层次 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 (Composition API) | 3.3.4 |
| 状态管理 | Pinia | 2.1.6 |
| 路由 | Vue Router | 4.2.4 |
| UI 组件库 | Element Plus | 2.3.12 |
| CSS 框架 | TailwindCSS | 3.3.3 |
| HTTP 客户端 | Axios | 1.5.0 |
| 图表 | ECharts | 5.4.3 |
| 构建工具 | Vite | 4.4.9 |
| 后端框架 | Spring Boot | 3.2.0 |
| AI 框架 | LangChain4j | 0.25.0 |
| 数据库 | MySQL (腾讯云 CynosDB) | 8.0 |
| 缓存 | Redis | 6.0 |
| AI 服务 | 通义千问 (DashScope) | qwen-plus |
| 容器化 | Docker + Docker Compose | - |
| CI/CD | Gitee Go 工作流 + 腾讯云 TCR + 腾讯云服务器主机组部署 | - |
| 反向代理 | Nginx | - |

### 系统架构

```
用户浏览器 → Nginx (80) → Vue3 SPA
                        → /api → Spring Boot (8080) → MySQL (云数据库)
                        → /ws  → WebSocket           → Redis (缓存)
                                                      → 通义千问 API
```

---

## 快速开始

### 环境要求

- Node.js 18+
- JDK 17+
- MySQL 8.0
- Redis 6.0

### 本地开发

```bash
# 1. 克隆项目
git clone <仓库地址>
cd ai-based-healthy-diet

# 2. 初始化数据库
mysql -u root -p -e "CREATE DATABASE nutriai CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p nutriai < backend/src/main/resources/db/alldata.sql

# 3. 启动后端
cd backend
# 编辑 src/main/resources/application.yml 配置数据库、Redis、通义千问 API Key
mvn clean compile
mvn spring-boot:run
# 后端运行在 http://localhost:8080

# 4. 启动前端
cd ../frontend
npm install
npm run dev
# 前端运行在 http://localhost:3000
```

### Docker Compose 本地运行

```bash
cp .env.example .env
# 编辑 .env 填入实际配置
docker compose up -d
# 前端: http://localhost  后端: http://localhost:8080
```

---

## 项目结构

```
ai-based-healthy-diet/
├── frontend/                 # Vue 3 前端
│   ├── src/
│   │   ├── views/            # 页面视图
│   │   ├── components/       # 可复用组件
│   │   ├── stores/           # Pinia 状态管理
│   │   ├── router/           # 路由配置
│   │   ├── services/         # API 服务层
│   │   ├── composables/      # 组合式函数
│   │   └── utils/            # 工具函数
│   ├── nginx/default.conf    # Nginx 配置
│   └── Dockerfile
├── backend/                  # Spring Boot 后端
│   ├── src/main/java/com/    # Java 源码
│   │   ├── controller/       # REST 控制器
│   │   ├── service/          # 业务逻辑
│   │   ├── repository/       # 数据访问
│   │   ├── model/            # 实体模型
│   │   ├── config/           # 配置类
│   │   └── security/         # 安全认证
│   ├── src/main/resources/
│   │   ├── application.yml   # 主配置
│   │   ├── application-prod.yml  # 生产配置
│   │   └── db/alldata.sql    # 数据库初始化脚本
│   └── Dockerfile
├── .workflow/
│   └── pipeline-docker.yml   # Gitee Go 工作流
├── Dockerfile.backend.gitee  # Gitee Go 后端构建包装 Dockerfile
├── Dockerfile.frontend.gitee # Gitee Go 前端构建包装 Dockerfile
├── docker-compose.yml        # 本地开发环境
├── docker-compose.prod.yml   # 生产环境
└── .env.example              # 环境变量模板
```

---

## 功能模块

### 用户端

| 模块 | 功能 |
|------|------|
| 首页 | 响应式导航栏、轮播 Banner、功能入口卡片、深色/浅色主题切换 |
| 用户认证 | 注册/登录、JWT Token 认证、连续失败后图形验证码、路由守卫 |
| 个人中心 | 健康档案（BMI 自动计算）、头像上传裁剪、密码修改 |
| 饮食记录 | 拖拽上传照片、营养摄入趋势图表（ECharts）、日/周/月统计 |
| AI 对话 | WebSocket 实时对话、流式输出、Markdown 渲染、DOMPurify XSS 过滤 |
| 饮食计划 | 3/7/30 天个性化方案生成、收藏管理、PDF 导出（会员专属） |
| 食物识别 | 拍照/上传识别食材、自动计算营养成分 |
| 会员中心 | 三级会员体系、成长值折线图、权益列表、专属邀请链接 |

### 管理端

| 模块 | 功能 |
|------|------|
| 数据看板 | 实时统计（WebSocket 更新）、用户增长趋势、AI 调用分布 |
| 用户管理 | 搜索/查看/编辑/禁用用户、角色分配 |
| AI 日志 | 调用记录查询、状态筛选、响应时间分析 |
| 公告管理 | 系统公告发布与管理 |
| 系统配置 | 运行参数动态配置 |

---

## 数据库设计

数据库名: `nutriai`，完整建表脚本: `backend/src/main/resources/db/alldata.sql`

### 核心表

| 模块 | 表名 | 说明 |
|------|------|------|
| 用户 | `users` | 用户基本信息（BCrypt 加密密码） |
| 用户 | `user_profiles` | 健康档案（身高/体重/BMI/健康目标） |
| 用户 | `user_allergies` | 过敏源记录 |
| 认证 | `login_logs` | 登录日志 |
| 会员 | `memberships` | 会员等级与 AI 配额 |
| 会员 | `growth_records` | 成长值变化记录 |
| 会员 | `invitation_records` | 邀请记录与奖励 |
| 饮食 | `diet_records` | 饮食记录 |
| 饮食 | `diet_plans` | AI 生成的饮食计划 |
| 饮食 | `favorite_plans` | 收藏的计划 |
| AI | `chat_messages` | 聊天消息记录 |
| AI | `ai_call_logs` | AI 调用日志 |
| 营养 | `nutrition_data` | 食材营养数据库 |
| 营养 | `food_categories` | 食材分类 |
| 系统 | `system_configs` | 系统配置 |
| 系统 | `admin_users` | 管理员 |
| 系统 | `announcements` | 系统公告 |

### 命名规范

- 表名: 小写 + 下划线，复数形式（`users`, `diet_records`）
- 字段: 小写 + 下划线（`user_id`, `created_at`）
- 索引: `idx_表名_字段名`
- 所有表含通用字段: `id`, `created_at`, `updated_at`, `is_deleted`（逻辑删除）

---

## API 接口

**Base URL**: `/api`  
**认证方式**: `Authorization: Bearer <JWT Token>`

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1701518400000
}
```

### 接口概览

#### 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/register` | 用户注册 |
| POST | `/auth/login` | 用户登录 |
| POST | `/auth/logout` | 退出登录 |
| POST | `/auth/refresh-token` | 刷新 Token |

#### 用户

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/profile` | 获取用户信息 |
| PUT | `/user/profile` | 更新用户资料 |
| PUT | `/user/change-password` | 修改密码 |

#### AI 功能

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/ai/generate-plan` | 生成饮食计划 |
| WebSocket | `/ws/chat` | AI 实时对话（流式输出） |
| GET | `/ai/chat-history` | 获取聊天历史 |
| POST | `/ai/recognize-food` | 食物图像识别 |
| POST | `/ai/export-pdf` | 导出饮食计划 PDF |

#### 会员

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/membership/info` | 获取会员信息 |
| GET | `/membership/growth-records` | 成长值记录 |
| POST | `/membership/generate-invite-link` | 生成邀请链接 |
| GET | `/membership/invite-records` | 邀请记录 |

#### 饮食记录

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/diet/records` | 创建饮食记录 |
| GET | `/diet/records` | 获取饮食记录 |
| DELETE | `/diet/records/{id}` | 删除饮食记录 |
| GET | `/diet/nutrition-trend` | 营养摄入趋势 |

#### 营养数据

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/nutrition/search` | 搜索食材 |
| GET | `/nutrition/{foodId}` | 食材详情 |
| GET | `/nutrition/categories` | 食材分类树 |

#### 后台管理（需管理员权限）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/admin/dashboard/metrics` | 数据看板 |
| GET | `/admin/users` | 用户管理列表 |
| GET | `/admin/ai-logs` | AI 调用日志 |

---

## 生产部署

### 架构概述

- **CI/CD**: Gitee Go 在代码推送后生成 14 位时间戳 Tag，构建后端与前端镜像并推送到腾讯云 TCR
- **镜像命名**: 固定仓库名 + 时间戳 Tag，格式为 `YYYYMMDDHHMMSS`
- **部署方式**: Gitee Go 通过腾讯云服务器上的主机组 Agent 执行远端部署，自动拉取本次时间戳镜像并重建容器
- **数据库**: 腾讯云 MySQL（CynosDB），SSL 加密连接
- **缓存**: Docker 容器内 Redis（生产默认使用阿里云公共镜像）
- **资源约束**: 容器最大内存、swap 等限制继续由 `docker-compose.prod.yml` 统一控制

### 1. 服务器初始化（仅首次）

```bash
ssh deployer@<服务器IP>
sudo apt update && sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker deployer
sudo mkdir -p /www/wwwroot/nutriai
```

还需要在该腾讯云服务器上加入 Gitee Go 主机组，供部署阶段远端执行。Gitee Go 官方文档公开的是“主机组 Agent”方案，而不是单独的 SSH 插件；实际效果等价于由工作流远端登录该服务器执行部署命令。

### 2. 配置服务器环境变量

在服务器 `/www/wwwroot/nutriai/` 创建 `.env` 文件（参考仓库根目录 `.env.example`）：

```bash
sudo nano /www/wwwroot/nutriai/.env
sudo chmod 600 /www/wwwroot/nutriai/.env
```

`.env` 文件内容：

```ini
# 腾讯云 TCR
TCR_REGISTRY=ccr.ccs.tencentyun.com
TCR_NAMESPACE=your_tcr_namespace
BACKEND_IMAGE_REPO=nutriai-backend
FRONTEND_IMAGE_REPO=nutriai-frontend
REDIS_IMAGE=swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/redis:6.0-alpine

# Docker 镜像标签（Gitee Go 部署阶段会自动更新为本次构建的时间戳 tag）
IMAGE_TAG=20260101000000

# 云数据库 (腾讯云 MySQL)
DB_HOST_NEI=10.0.0.10
DB_PORT_NEI=3306
DB_NAME=nutriai
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# Redis
REDIS_PASSWORD=your_redis_password

# JWT 认证（openssl rand -base64 64 生成）
JWT_SECRET=your_jwt_secret_key_at_least_64_characters_long

# 通义千问 AI
AI_API_KEY=sk-your_ai_api_key
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_MODEL_NAME=qwen3.5-122b-a10b
AI_DIET_PLAN_MODEL=qwen3.5-flash

# CORS
CORS_ALLOWED_ORIGINS=https://your-domain.com

# Java 运行参数
JAVA_OPTS=-Xms256m -Xmx384m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Duser.timezone=Asia/Shanghai

# 邮件
MAIL_HOST=smtp.example.com
MAIL_PORT=465
MAIL_USERNAME=your_mail_username
MAIL_PASSWORD=your_mail_password
MAIL_NICKNAME=NutriAI健康饮食助手

# 腾讯云 COS 对象存储（可选）
COS_SECRET_ID=
COS_SECRET_KEY=
COS_REGION=ap-beijing
COS_BUCKET=nutriai-xxxxxxxx
```

### 3. 配置 Gitee Go 与腾讯云 TCR

详细字段说明见 `docs/gitee-go-tcr-deploy.md`。至少需要创建两个腾讯云 TCR 镜像仓库：

- `<命名空间>/nutriai-backend`
- `<命名空间>/nutriai-frontend`

Gitee Go 工作流依赖以下全局参数或流水线参数：

- `TCR_NAMESPACE`
- `TCR_USERNAME`
- `TCR_PASSWORD`
- `DEPLOY_HOST_GROUP_ID`

其中镜像 Tag 由工作流自动生成，格式固定为 `YYYYMMDDHHMMSS`。

### 4. 初始化云数据库

通过 MySQL 客户端连接云数据库，导入建表脚本：

```bash
mysql -h <DB_HOST> -P <DB_PORT> -u <DB_USERNAME> -p --ssl-mode=REQUIRED \
  nutriai < backend/src/main/resources/db/alldata.sql
```

### 5. 触发构建与部署

1. 将代码推送到 Gitee 仓库。
2. 确认仓库内存在 `.workflow/pipeline-docker.yml`，然后打开对应的 Gitee Go 流水线页面，选择 `nutriai-gitee-go-tcr`。
3. 点击“执行流水线”，选择需要部署的分支，生产建议选择 `master`。
4. Gitee Go 会生成新的时间戳镜像标签，完成构建、推送、远端部署和清理。

如果 Gitee Go 列表中没有识别到流水线，先确认两件事：
1. 该仓库已经在 Gitee 页面开通 Gitee Go。
2. 默认分支已经包含 `.workflow/pipeline-docker.yml`。

确认后再刷新 Gitee Go 页面；官方示例和帮助文档使用的也是 `.workflow` 目录。

### 6. 部署流程（自动）

1. 在 Gitee Go 页面手动执行流水线后，系统生成当前运行对应的 14 位时间戳 Tag。
2. 工作流使用两份 Gitee 专用包装 Dockerfile 构建后端/前端镜像并推送到腾讯云 TCR。
3. 工作流把最新的 `docker-compose.prod.yml` 下发到腾讯云服务器。
4. 远端部署步骤登录 TCR，更新 `.env` 中的 `IMAGE_TAG` 与镜像仓库变量。
5. 远端部署步骤执行 `docker compose pull` 与 `docker compose up -d --force-recreate --remove-orphans`。
6. 远端部署步骤清理旧的停止容器、旧的时间戳镜像以及悬空镜像。

### 7. 运维命令

```bash
ssh deployer@<服务器IP>
cd /www/wwwroot/nutriai

# 查看容器状态
docker compose --env-file .env -f docker-compose.prod.yml ps

# 查看后端日志
docker compose --env-file .env -f docker-compose.prod.yml logs -f backend

# 查看前端日志
docker compose --env-file .env -f docker-compose.prod.yml logs -f frontend
```

---

## 开发规范

### Git 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat: 新功能          fix: Bug 修复
docs: 文档更新        style: 代码格式
refactor: 代码重构    perf: 性能优化
test: 测试相关        chore: 构建/工具变动
```

### 分支策略

- `master` — 生产分支，受保护
- `develop` — 开发分支
- `feature/*` — 功能分支，从 develop 创建，合并回 develop

### CI/CD 流程

- **Manual Run in Gitee Go**: 在流水线页面手动点击执行，并选择目标分支
- **Push to TCR**: 工作流将本次时间戳镜像推送到 `ccr.ccs.tencentyun.com/<namespace>/nutriai-backend` 与 `nutriai-frontend`
- **Deploy on Tencent CVM**: Gitee Go 主机组部署步骤在目标服务器执行 `docker compose pull/up`，按 compose 中的内存上限重建容器
- **Cleanup**: 工作流自动清理 `nutriai` 项目的旧停止容器、旧时间戳镜像和悬空镜像

---

## 常见问题

**如何获取通义千问 API Key？**  
访问 [阿里云 DashScope](https://dashscope.aliyun.com/) 注册并申请免费额度。

**首次运行数据库连接失败？**  
确认 MySQL 服务已启动，检查 `application.yml` 中的数据库连接配置。

**前端页面空白？**  
检查前端 `.env` 文件中 `VITE_API_BASE_URL` 是否正确指向后端地址。

**AI 响应速度慢？**  
1. 确认通义千问 API 额度未耗尽  
2. 检查服务器到阿里云的网络延迟  
3. Redis 缓存可减少重复查询

---

## License

MIT

