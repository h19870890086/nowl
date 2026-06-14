<div align="center">

# 🌙 Nowl · 校园商品 × 跑腿 × AI 一体化平台

<p>
  <strong>Night Owl（夜猫子）校园交易平台</strong><br/>
  把「二手交易」「校园跑腿」「AI 助手」「钱包充值」「后台治理」融合在一个完整系统中
</p>

<p>
  <img src="https://img.shields.io/badge/Frontend-Vue%203%20%2B%20TS-42b883?style=for-the-badge" alt="frontend" />
  <img src="https://img.shields.io/badge/Backend-Spring%20Boot%203-6db33f?style=for-the-badge" alt="backend" />
  <img src="https://img.shields.io/badge/ORM-MyBatis--Plus%203.5-red?style=for-the-badge" alt="orm" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="license" />
</p>

<p>
  <a href="#-项目亮点">项目亮点</a> ·
  <a href="#-功能矩阵">功能矩阵</a> ·
  <a href="#-快速开始">快速开始</a> ·
  <a href="#-系统架构">系统架构</a> ·
  <a href="#-文档导航">文档导航</a>
</p>

</div>

---

## ✨ 项目亮点

- **场景融合**：商品交易 + 跑腿服务 + AI 互动推荐 + 钱包充值 + 社交关注，覆盖校园高频需求
- **治理完善**：IAM 多级权限体系（7角色×33权限点）、风控引擎（规则/黑白名单/行为管控）、审计追踪、纠纷闭环
- **信用机制**：基于交易行为的五级信用评估（优秀/良好/一般/较差/危险），贯穿下单、评价、纠纷全链路
- **工程化完整**：前后端分层清晰，后端 8 模块微服务，中间件按需启用，支持 Docker 一键部署
- **学习价值高**：适合作为课程设计、毕业设计或中大型全栈项目实践样板

---

## 🖼 界面预览

<p>
  <img src="docs/assets/home.png" width="48%" alt="首页" />
  <img src="docs/assets/market.png" width="48%" alt="集市" />
</p>
<p>
  <img src="docs/assets/errand.png" width="48%" alt="跑腿" />
  <img src="docs/assets/admin.png" width="48%" alt="后台管理" />
</p>
<p>
  <img src="docs/assets/ChatAI.png" width="48%" alt="AI 对话" />
</p>

---

## 🧩 功能矩阵

### 一、用户侧能力

#### 🔐 账号体系
- **注册登录**：邮箱验证码注册 + 账号密码登录，双 Token 机制（Access Token + Refresh Token）通过 httpOnly Cookie 安全传递
- **邮箱验证码**：QQ 邮箱 SMTP 发送 6 位数字验证码，Redis 存储有效期 5 分钟，60 秒发送限流防滥用；开发环境支持验证码打印日志（无需真实 SMTP 配置）
- **密码重置**：短信验证码找回密码
- **实名认证**：用户提交真实姓名 + 身份证号 + 学生证照片，管理员后台审核（通过/拒绝）
- **跑腿员认证**：独立申请通道，审核通过后获得接单资格

#### 💰 钱包与充值
- **账户余额**：用户初始余额 0.00 元，支持模拟充值（微信/支付宝两种支付方式声明）
- **充值金额**：预设 10/20/50/100/200 元档位 + 自定义金额
- **原子操作**：`UPDATE user_info SET money = money + #{amount}` 保证并发安全
- **资金流转**：买家下单扣款 → 平台托管 → 确认收货转入卖家；跑腿发布扣赏金 → 完成转入跑腿员
- **充值记录**：完整流水记录（金额、支付方式、状态、时间），充值成功自动发系统通知

#### 🛒 商品交易
- **发布链路**：选择分类 → 填写信息 → 上传图片 → 风控检测 → AI 自动审核 →（必要时）人工复核 → 上架
- **商品状态**：在售 / 已售出 / 已下架 / 软删除
- **商品分类**：10 个一级分类 × 60 个二级分类，覆盖校园二手交易全场景（见下方分类表）
- **搜索浏览**：Elasticsearch 全文检索 + IK 中文分词 + 拼音搜索，支持分类/价格/学校/校区筛选，5 种排序方式

#### 📦 订单链路
- **下单**：防超卖锁定检查，余额扣款后资金托管
- **状态流转**：待支付 → 待发货 → 待收货 → 已完成 / 已取消 / 已结束
- **退款机制**：支持退款申请，状态独立追踪（处理中 / 已退款 / 拒绝）

#### 🏃 跑腿服务
- **任务发布**：填写取件/送达地址 + 任务内容 + 赏金金额 + 备注
- **审核流程**：AI 自动审核 → 人工复核（同商品审核链路）
- **任务状态**：待接单 → 进行中 → 待确认 → 已完成 / 已取消
- **接单互斥**：一个任务只能被一人接单，防止重复接单
- **履约流程**：接单人上传凭证 → 发布人确认完成 → 赏金自动转入跑腿员账户
- **延迟自动确认**：超时未确认自动完成，保护跑腿员权益

#### 💬 社交与评价
- **关注体系**：关注 / 取消关注 / 粉丝列表，关注动态推荐
- **用户拉黑**：拉黑后双方无法私聊，被拉黑者无法查看商品
- **私聊功能**：WebSocket 实时通信，支持离线消息 Redis 缓存（24 小时过期）
- **评价系统**：订单评价 + 跑腿评价，双向互评（买家↔卖家、发布者↔跑腿员），评分 1-5 星，关联信用等级
- **纠纷处理**：商品纠纷 + 跑腿纠纷，状态：待处理 → 处理中 → 已解决 / 已驳回 / 已撤回

#### 🤖 AI 能力
- **AI 对话**：接入 Kimi / Moonshot 大模型，支持校园场景问答
- **商品推荐**：基于协同过滤，在首页和商品详情页推荐相关商品
- **智能审核**：AI 自动审核商品内容和跑腿任务（敏感词检测、违规内容识别）

#### 📬 通知中心
- **通知类型**：系统通知 / 交易通知 / 评价通知 / 纠纷通知
- **推送机制**：消息落库 + WebSocket 实时推送，`bizType` 区分业务类型支持跳转
- **未读计数**：红点角标，支持单条已读和全部已读

---

### 二、平台治理能力（管理后台）

#### 👤 IAM 权限体系
7 种预设角色 × 33 个精细权限点 × 3 层管理范围：

| 角色 | 级别 | 说明 |
|------|------|------|
| SUPER_ADMIN | 1 | 平台超级管理员，全部权限 + 全平台范围 |
| SCHOOL_ADMIN | 20 | 学校管理员，管理本校数据 |
| CAMPUS_ADMIN | 30 | 校区管理员，管理指定校区数据 |
| CONTENT_AUDITOR | 40 | 内容审核员，审核商品和跑腿 |
| RISK_OPERATOR | 50 | 风控运营，管理风控规则和工单 |
| CUSTOMER_SUPPORT | 60 | 客服专员，处理纠纷和用户问题 |
| FINANCE_AUDITOR | 70 | 财务审核员，审核交易流水 |

**权限点分组**：看板 | 商品 | 用户 | 认证审核 | 跑腿员审核 | 订单 | 纠纷 | 跑腿任务 | IAM 管理 | 审计 | 风控（共 33 个权限点）

**管理范围三层模型**：ALL（全平台）→ SCHOOL（指定学校）→ CAMPUS（指定校区），下级管理员无法越权访问上级范围数据。

**多租户隔离**：MyBatis-Plus 租户插件自动按 `school_code` 过滤商品和跑腿数据，管理后台使用 IAM 范围替代租户过滤。

#### 🛡️ 风控引擎
**三种运行模式**：OFF（关闭）/ BASIC（基础：黑白名单+行为管控）/ FULL（完整：+规则引擎）

**五级动作**：ALLOW（放行）→ REJECT（拒绝）→ CHALLENGE（安全校验）→ REVIEW（人工复核，创建工单）→ LIMIT（限流）

**事件类型**（7 种）：登录、商品发布、跑腿发布、跑腿接单、私聊、AI 对话、关注用户

**预置规则**（5 条）：

| 规则 | 类型 | 阈值/关键词 | 动作 |
|------|------|-----------|------|
| 登录频控 | 阈值 | 10分钟内同IP >15次 | LIMIT |
| 商品发布频控 | 阈值 | 60分钟内同用户 >20次 | LIMIT |
| 跑腿发布频控 | 阈值 | 60分钟内同用户 >30次 | LIMIT |
| 私聊敏感词 | 关键词 | 加微信/vx/刷单/博彩/毒品/代考 | REVIEW |
| AI对话敏感词 | 关键词 | 代写论文/代考/博彩/毒品/办证 | REVIEW |

**黑白名单**：支持按用户/IP 维度设置，可配置过期时间，白名单优先于黑名单。

#### 📊 审计中心
- **管理员操作日志**：记录所有管理后台操作（操作人、时间、IP、操作内容、结果）
- **登录轨迹**：追踪所有登录行为（用户、IP、设备、时间、成功/失败）
- **权限变更记录**：记录角色绑定/解绑、范围变更等 IAM 操作
- **风控事件记录**：所有风控事件和决策结果异步持久化

#### 🔍 搜索管理
- **索引管理**：全量同步 + 索引重建（需 SUPER_ADMIN 权限）
- **热搜管理**：基于 Redis ZSet 的全局热搜和按学校热搜，搜索次数排序
- **搜索历史**：每用户最多 10 条，Redis List 存储，30 天自动过期
- **降级策略**：ES 异常时自动降级到 MySQL 直接查询

---

### 三、商品分类覆盖

10 个一级分类、60 个二级分类，涵盖校园二手交易全场景：

| 一级分类 | 二级分类 |
|---------|---------|
| 教辅教材 | 专业课教材、公共课教材、考研教材、考公教材、语言考试资料、笔记讲义 |
| 电子产品 | 手机、笔记本电脑、平板电脑、耳机音箱、相机摄影、**游戏设备** |
| 电子资料 | 课程PPT/讲义、历年真题、考研网课资料、考公网课资料、编程开发资料、设计素材模板 |
| 生活用品 | 收纳整理、床上用品、洗护用品、厨房餐具、清洁用品、雨伞水杯 |
| 学习办公 | 文具用品、台灯护眼、计算器、打印设备、桌椅支架、文件夹活页 |
| 服饰鞋包 | 上衣外套、裤装裙装、运动鞋、包袋书包、配饰、正装礼服 |
| 运动健身 | 球类用品、羽网乒器材、跑步装备、力量训练、瑜伽普拉提、骑行滑板 |
| 美妆个护 | 护肤品、彩妆、香水香氛、个护电器、美发造型、护理工具 |
| 宿舍电器 | 小风扇、电煮锅、加湿器、电热毯、吹风机、路由器 |
| 文娱周边 | 书籍小说、乐器、手办潮玩、**桌游卡牌**、演出票券、社团活动物资 |

---

## 🏗 系统架构

### 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| 前端框架 | Vue 3 + TypeScript | 3.5 |
| 状态管理 | Pinia | 3.0 |
| 构建工具 | Vite | 7.3 |
| CSS 框架 | Tailwind CSS | 3.x |
| 后端框架 | Spring Boot | 3.x |
| ORM | MyBatis-Plus | 3.5.7 |
| 数据库 | MySQL | 8.0 |
| 缓存 | Redis | 7 |
| 消息队列 | RocketMQ | 5.1.4 |
| 搜索引擎 | Elasticsearch | 8.13.0（IK + 拼音分词器） |
| 定时任务 | XXL-JOB | 2.4.0 |
| 容器化 | Docker + Docker Compose | — |

### 仓库结构

```text
Nowl/
├── Nowl-front/        # Vue3 前端（39 条路由，46 个视图文件）
├── Nowl-backend/      # Spring Boot 多模块后端
│   ├── unimarket-web          # 业务主模块：商品/订单/跑腿/纠纷/AI/社交/通知/学校
│   ├── unimarket-security     # 安全认证：JWT + 业务鉴权 + 多租户
│   ├── unimarket-core         # 核心基础：充值/文件/邮件/风控/IAM实体/异常处理
│   ├── unimarket-admin        # 管理后台：仪表盘/审核/IAM/风控/审计/搜索管理
│   ├── unimarket-search       # 搜索服务：ES 检索/拼音/热搜/历史/降级
│   ├── unimarket-recommend    # 推荐服务：协同过滤/用户偏好/离线计算
│   ├── unimarket-ai           # AI 服务：对话/推荐/审核
│   └── unimarket-gateway      # API 网关：认证/审计/限流/追踪/CORS
├── sql/               # 数据库初始化脚本（建库建表+角色权限+风控规则+分类种子数据）
├── docker/            # Docker 辅助配置（ES 中文分词镜像/ RocketMQ Broker配置）
└── docs/              # 系统设计说明书 / 中间件部署文档 / 简历项目经历 / 风控压测说明
```

### 数据模型（33 个实体）

| 领域 | 实体 |
|------|------|
| 用户与社交 | user_info, user_follow, chat_message, chat_block_record |
| 商品与订单 | goods_info, order_info, collection_record, item_category |
| 跑腿 | errand_task |
| 纠纷与评价 | dispute_record, review_record |
| 通知 | sys_notice |
| 钱包 | recharge_record |
| 学校 | school_info |
| AI | ai_chat_history |
| IAM 权限 | iam_role, iam_permission, iam_role_permission, iam_user_role, iam_admin_scope_binding |
| 审计 | audit_admin_operation, audit_login_trace, audit_permission_change |
| 风控 | risk_rule, risk_decision, risk_event, risk_case, risk_blacklist, risk_whitelist, risk_behavior_control |
| 推荐 | user_preference, user_behavior_log, goods_similarity |

---

## 🚀 快速开始

### 1) 环境要求

- JDK 17+
- Maven 3.9+
- Node.js 18+（建议）与 npm
- Docker & Docker Compose（推荐）

### 2) Docker 一键部署（推荐）

```bash
# 启动全部服务
docker-compose up -d
```

自动启动：MySQL + Redis + RocketMQ（Namesrv + Broker）+ Elasticsearch + XXL-JOB + 后端 + 网关 + 前端

| 服务 | 地址 |
|------|------|
| 前端 | `http://localhost:3000`（可通过 `FRONTEND_PORT` 环境变量自定义） |
| 网关 | `http://localhost:8090` |
| 后端 | `http://localhost:8080` |
| XXL-JOB | `http://localhost:8082` |
| ES | `http://localhost:9200` |

### 3) 本地开发模式

```bash
# 只启动中间件
docker-compose up -d mysql redis

# 后端
cd Nowl-backend
mvn -q -DskipTests -pl unimarket-web -am spring-boot:run

# 前端
cd Nowl-front
npm install
npm run dev
```

开发环境访问：前端 `localhost:5173` · 网关 `localhost:8090` · 后端 `localhost:8080`

### 4) 环境变量

参考根目录 `.env.example`。必需项：

| 变量 | 说明 |
|------|------|
| `DB_URL` `DB_USERNAME` `DB_PASSWORD` | 数据库连接 |
| `REDIS_HOST` `REDIS_PORT` `REDIS_PASSWORD` | Redis 连接 |
| `JWT_SECRET` | JWT 签名密钥 |

可选项（不配置则对应功能降级）：

| 变量 | 对应功能 |
|------|---------|
| `OPENAI_API_KEY` `OPENAI_BASE_URL` `OPENAI_MODEL` | AI 对话与审核 |
| `COS_SECRET_ID` `COS_SECRET_KEY` `COS_REGION` `COS_BUCKET_NAME` | 腾讯云对象存储 |
| `ROCKETMQ_NAME_SERVER` | 异步审核/索引同步/延迟任务 |
| `ES_HOST` | Elasticsearch 检索 |
| `XXL_JOB_ADMIN_ADDRESSES` | 离线推荐任务调度 |
| `MAIL_ENABLED` `MAIL_HOST` … `MAIL_PASSWORD` | 邮箱验证码 |
| `SMS_ENABLED` `SMS_SPUG_URL` | 短信验证码 |

---

## 🔐 管理员初始化

初始化 SQL 预置 7 种角色和 33 个权限点，需手动绑定管理员：

```sql
-- 绑定超管角色
INSERT INTO iam_user_role(user_id, role_id, status)
SELECT 123, role_id, 1 FROM iam_role
WHERE role_code = 'SUPER_ADMIN'
ON DUPLICATE KEY UPDATE status = 1;

-- 绑定全平台管理范围
INSERT INTO iam_admin_scope_binding(user_id, scope_type, status)
VALUES (123, 'ALL', 1);
```

> 将 `123` 替换为实际用户 ID。

---

## 📚 文档导航

| 文档 | 路径 | 内容 |
|------|------|------|
| 系统设计说明书 | `docs/Nowl系统设计说明书.md` | 完整系统设计、ER 图、接口文档 |
| 中间件部署文档 | `docs/中间件部署文档.md` | MySQL/Redis/ES/RocketMQ/XXL-JOB 部署指南 |
| 项目经历说明 | `docs/简历项目经历-Nowl.md` | 简历项目描述参考 |
| 风控压测说明 | `docs/风控压测说明.md` | 风控规则配置与压力测试 |

---

## 🤝 适用人群

- 想做**毕业设计 / 课程设计**的同学——功能完整、文档齐全、答辩友好
- 想学习 **Vue 3 + Spring Boot 3 全栈工程化**的开发者——前后端分离、多模块架构、中间件实战
- 想了解**校园交易 + 平台治理 + 风控体系**落地方式的同学——真实业务场景全覆盖

如果这个项目对你有帮助，欢迎点个 **Star** ⭐

---

## 📄 License

MIT License，详见 `LICENSE`。
