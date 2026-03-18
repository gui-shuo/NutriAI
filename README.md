# AI健康饮食规划助手系统

**项目代号**: NutriAI-Pilot  
**版本**: 2.0  
**开发周期**: 12周  
**最后更新**: 2025年12月2日

---

## 📖 项目简介

AI健康饮食规划助手是一个基于Vue3+Spring Boot+通义千问AI的智能健康管理平台。系统通过AI技术将专业营养学知识转化为可执行的日常饮食计划，解决都市人群"吃得健康却不知如何搭配"的核心痛点。

### 核心特性

- ✅ **AI智能规划**: 基于通义千问API，3分钟生成个性化饮食方案
- ✅ **实时对话**: WebSocket实时通信，流式输出AI回复
- ✅ **会员成长体系**: 游戏化设计，激励健康习惯养成
- ✅ **营养数据库**: 3000+食材营养数据，精准计算摄入
- ✅ **数据可视化**: ECharts图表展示营养趋势和成长曲线
- ✅ **响应式设计**: 完美适配Mobile/Tablet/Desktop

---

## 🎯 项目背景

### 用户痛点
- **73%** 的都市人群存在饮食结构不合理问题
- **专业营养师** 服务覆盖率不足2%，费用高昂（300-800元/次）
- **知识匮乏**: 不知道如何科学搭配三餐
- **坚持困难**: 缺乏激励机制和习惯养成支持

### 解决方案
通过AI技术将专业营养咨询服务成本降低95%以上，从传统的1-2小时人工咨询缩短到3分钟AI生成，同时提供会员成长体系和数据可视化分析，帮助用户养成健康饮食习惯。

---

## 🏗️ 技术架构

### 技术栈总览

**前端技术栈**:
```
Vue 3.3.4 + Composition API
├── 状态管理: Pinia 2.1.6
├── 路由管理: Vue Router 4.2.4
├── UI组件库: Element Plus 2.3.12
├── CSS框架: TailwindCSS 3.3.3
├── HTTP客户端: Axios 1.5.0
├── 图表库: ECharts 5.4.3
├── Markdown渲染: marked + DOMPurify
└── 构建工具: Vite 4.4.9
```

**后端技术栈**:
```
Spring Boot 3.2.0
├── AI框架: LangChain4j 0.25.0
├── 数据库: MySQL 8.0
├── 缓存: Redis 6.0
├── 对象存储: 阿里云OSS
├── AI服务: 通义千问API (qwen-max)
└── 图像识别: 阿里云图像识别API
```

### 系统架构图

```
用户层 → CDN → Nginx → [Vue3前端 + SpringBoot后端] → [MySQL + Redis] → 通义千问API
```

详细架构设计请查看 [技术架构文档](./docs/04-技术架构设计.md)

---

## 🚀 快速开始

### 环境要求

- **Node.js**: 18+
- **JDK**: 17+
- **MySQL**: 8.0
- **Redis**: 6.0
- **Git**: 最新版本

### 安装步骤

#### 1. 克隆项目
```bash
git clone [仓库地址]
cd ai-based-healthy-diet
```

#### 2. 前端安装
```bash
cd frontend
npm install
cp .env.example .env
# 编辑.env文件，配置API地址和通义千问API Key
npm run dev
```

访问 `http://localhost:3000`

#### 3. 后端安装
```bash
cd backend
# 配置application.yml中的数据库连接、Redis连接、通义千问API Key
mvn clean install
mvn spring-boot:run
```

访问 `http://localhost:8080`

#### 4. 数据库初始化
```bash
# 创建数据库
mysql -u root -p < docs/sql/V1__init_schema.sql

# 导入示例数据
mysql -u root -p nutriai < docs/sql/demo_data.sql
```

---

## 📚 文档中心

完整的项目文档位于 `docs/` 目录，包括：

| 文档 | 说明 |
|------|------|
| [01-系统概述与目标](./docs/01-系统概述与目标.md) | 项目背景、核心价值、目标体系 |
| [02-功能需求详述](./docs/02-功能需求详述.md) | 6大核心模块详细需求 |
| [03-前端开发规范](./docs/03-前端开发规范.md) | Vue3组件规范、评分标准、答辩指南 |
| [04-技术架构设计](./docs/04-技术架构设计.md) | 系统架构、部署方案 |
| [05-数据库设计](./docs/05-数据库设计.md) | 15张核心表、ER图、索引设计 |
| [06-接口文档](./docs/06-接口文档.md) | REST API、WebSocket接口规范 |
| [07-项目开发计划](./docs/07-项目开发计划.md) | 12周迭代计划、任务分解 |

详见 [文档中心](./docs/README.md)

---

## 🎨 核心功能

### 1. 首页模块
- 响应式导航栏（滚动样式变化）
- 自动轮播Banner（Vue Transition动画）
- 功能入口卡片
- 深色/浅色主题切换

### 2. 用户认证
- 注册/登录（VeeValidate表单验证）
- JWT Token认证
- 连续登录失败后图形验证码
- 路由守卫权限控制

### 3. 个人中心
- 健康档案管理（BMI自动计算）
- 饮食记录（拖拽上传照片）
- 营养摄入趋势图表（ECharts）
- 收藏的饮食计划

### 4. 会员中心
- 三级会员体系（普通/白银/黄金）
- 成长值折线图（ECharts）
- 权益列表（虚拟滚动优化）
- 专属邀请链接生成

### 5. AI功能（核心）
- **实时对话**: WebSocket通信，流式输出
- **饮食计划生成**: 3/7/30天个性化方案
- **营养分析**: 热量、蛋白质、碳水、脂肪计算
- **食物识别**: 上传照片自动识别营养成分
- **PDF导出**: 会员专属功能
- **安全防护**: DOMPurify XSS过滤

### 6. 后台管理
- 数据看板（实时WebSocket更新）
- 用户管理（查看/编辑/禁用）
- AI调用日志查询
- 权限控制（角色：运营/管理员/超级管理员）

---

## 📊 项目数据

### 数据库设计
- **15张核心表**: users, user_profiles, memberships, diet_records, chat_messages等
- **3000+食材**: 完整营养数据库
- **三级分类**: 主食类、蛋白质类、蔬菜类、水果类

### 性能指标
- **首屏加载**: < 1.5秒（3G网络）
- **AI响应**: < 3秒（95%请求）
- **并发支持**: ≥ 5000用户
- **系统可用性**: ≥ 99.5%

---

## 🧪 测试

### 运行测试

**前端测试**:
```bash
cd frontend
npm run test:unit # 单元测试
npm run test:e2e  # E2E测试
```

**后端测试**:
```bash
cd backend
mvn test          # 单元测试
mvn verify        # 集成测试
```

### 测试覆盖率
- **单元测试**: > 70%
- **集成测试**: 关键API 100%
- **E2E测试**: 核心用户流程覆盖

---

## 🚢 部署

### Docker部署（GitHub Actions 自动化，镜像在 Runner 本地构建）

适用于：火山引擎 Ubuntu 24.04（2vCPU / 2GiB）服务器。  
下文以 `115.190.164.107` 作为示例 IP（请替换为你的实际公网 IP）。  
核心思路：**在 GitHub Runner 构建前后端镜像**，上传镜像包到服务器后 `docker load`，避免在 2C2G 服务器上执行 `docker build`。

1) 服务器初始化（只需一次）

```bash
ssh deployer@115.190.164.107
sudo apt update
sudo apt install -y docker.io docker-compose-v2
sudo systemctl enable --now docker
```

2) 在 GitHub 仓库配置以下 Secrets

- `SERVER_HOST`: `115.190.164.107`
- `SERVER_USER`: 服务器登录用户（建议 `deployer` 这类普通用户，并加入 `sudo` 组）
- `SERVER_SSH_KEY`: 私钥内容
- `PROD_API_BASE_URL` / `PROD_WS_BASE_URL`
- `MYSQL_ROOT_PASSWORD` / `MYSQL_PASSWORD` / `REDIS_PASSWORD`
- `JWT_SECRET` / `TONGYI_API_KEY`
- `ALIYUN_OSS_ACCESS_KEY_ID` / `ALIYUN_OSS_ACCESS_KEY_SECRET`
- `BAIDU_AI_APP_ID` / `BAIDU_AI_API_KEY` / `BAIDU_AI_SECRET_KEY`

建议按下表填写（全部为 **GitHub Secrets**，不是变量名本身）：

| Secret 名称 | 填写内容 | 示例 |
|---|---|---|
| `PROD_API_BASE_URL` | 前端访问后端 API 的生产地址（通常带 `/api`） | `http://115.190.164.107/api` |
| `PROD_WS_BASE_URL` | 前端 WebSocket 生产地址（通常 `ws://.../ws`） | `ws://115.190.164.107/ws` |
| `MYSQL_ROOT_PASSWORD` | MySQL `root` 密码（用于容器初始化与健康检查） | `MysqL_root_2026!` |
| `MYSQL_PASSWORD` | 业务库用户 `nutriai_user` 的密码 | `NutriAI_db_2026!` |
| `REDIS_PASSWORD` | Redis 密码（`redis-server --requirepass`） | `Redis_2026_Strong!` |
| `JWT_SECRET` | JWT 签名密钥（建议 64+ 字节随机字符串） | `openssl rand -base64 64` 生成后粘贴 |
| `TONGYI_API_KEY` | 阿里云 DashScope（通义千问）API Key | `sk-xxxxxxxxxxxxxxxx` |
| `ALIYUN_OSS_ACCESS_KEY_ID` | 阿里云 RAM 用户 AccessKey ID（用于 OSS 上传） | `LTAI5txxxxxxxxxx` |
| `ALIYUN_OSS_ACCESS_KEY_SECRET` | 上述 AccessKey 对应的 Secret | `xxxxxxxxxxxxxxxx` |
| `BAIDU_AI_APP_ID` | 百度 AI 开放平台 应用 AppID（图像识别） | `12345678` |
| `BAIDU_AI_API_KEY` | 百度 AI 应用 API Key | `xxxxxxxxxxxxxxxx` |
| `BAIDU_AI_SECRET_KEY` | 百度 AI 应用 Secret Key | `xxxxxxxxxxxxxxxx` |

> 如果你暂时不用“百度图片识别”，可先不配置 `BAIDU_AI_*`（留空时后端不会初始化百度 AI 客户端）。

3) 触发部署

- 手动触发：GitHub Actions -> `Deploy to Volcano Engine` -> `Run workflow`
- 或推送版本标签：`git tag v1.0.0 && git push origin v1.0.0`

4) 工作流执行内容（`.github/workflows/deploy.yml`）

- 在 GitHub Runner 本地构建：
  - `nutriai-backend:<tag>`
  - `nutriai-frontend:<tag>`
- 打包镜像为 `nutriai-images.tar.gz` 并上传到服务器
- 上传 `docker-compose.prod.yml` 与 `.env`
- 服务器执行：
  - `docker load -i nutriai-images.tar.gz`
  - `docker compose --env-file .env -f docker-compose.prod.yml up -d`

> 2GiB 服务器建议预留内存分配：MySQL ~600MiB、Redis ~150MiB、Backend 堆上限 384MiB、Frontend/Nginx ~100MiB，其余留给系统缓存与 Docker 开销。

5) 服务器运维常用命令

```bash
ssh deployer@115.190.164.107
cd /opt/nutriai
sudo docker compose --env-file .env -f docker-compose.prod.yml ps
sudo docker compose --env-file .env -f docker-compose.prod.yml logs -f
sudo docker compose --env-file .env -f docker-compose.prod.yml pull mysql redis  # 仅更新基础镜像服务
```

### 生产环境部署

详见 [部署架构文档](./docs/04-技术架构设计.md#6-部署架构)

---

## 🤝 贡献指南

### 开发流程

1. Fork项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'feat: Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

### 提交规范

遵循 [Conventional Commits](https://www.conventionalcommits.org/)：

```
feat: 新功能
fix: Bug修复
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
perf: 性能优化
test: 测试相关
chore: 构建工具或辅助工具变动
```

---

## 📋 开发计划

### 当前进度

- [x] 需求分析与设计 （第1-2周）
- [ ] 核心模块开发 （第3-6周）
- [ ] AI功能开发 （第7-9周）
- [ ] 后台管理与优化 （第10-11周）
- [ ] 部署与答辩 （第12周）

详见 [项目开发计划](./docs/07-项目开发计划.md)

---

## ❓ 常见问题

### Q1: 如何获取通义千问API Key？
访问 [阿里云官网](https://dashscope.aliyun.com/) 注册并申请免费额度。

### Q2: 首次运行提示数据库连接失败？
检查 `application.yml` 中的数据库配置，确保MySQL服务已启动。

### Q3: 前端页面显示空白？
检查 `.env` 文件中的 `VITE_API_BASE_URL` 是否正确配置。

### Q4: AI响应速度慢？
1. 检查通义千问API额度是否用完
2. 检查网络连接
3. 考虑启用Redis缓存

更多问题请查看 [FAQ文档](./docs/FAQ.md)

---

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](./LICENSE) 文件。

---

## 🎓 致谢

- 感谢《前端开发技术》课程组提供的考核方案
- 感谢阿里云提供通义千问API支持
- 感谢Vue、Spring Boot等开源社区

---

## 📞 联系我们

- **项目负责人**: [待定]
- **技术支持**: [待定]
- **问题反馈**: [GitHub Issues](仓库地址/issues)

---

**最后更新**: 2025年12月2日  
**维护团队**: AI健康饮食规划助手项目组

---

> 💡 **提示**: 这是一个用于《前端开发技术》期末考核的完整项目，包含Vue3全栈开发、AI集成、数据可视化等多项技术亮点。建议配合 [文档中心](./docs/README.md) 阅读使用。
