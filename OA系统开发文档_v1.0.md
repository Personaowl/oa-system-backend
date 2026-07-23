JAVAEE 企业级开发 · 课程项目
# OA 办公管理系统
总体开发文档与一周交付计划
Spring Boot 3 微服务 · Vue 3 · Nacos · Redis · Elasticsearch · Spring AI
用途：团队开发、联调、测试、验收与答辩的统一执行依据

# 文档导航
1. 文档目标与项目成功标准
2. 课程要求追踪与范围基线
3. 技术基线与总体架构
4. 后端仓库与 Maven 多模块结构
5. 微服务职责与接口边界
6. 数据模型与存储设计
7. 认证授权与安全设计
8. 中间件与配置规范
9. API 与错误码规范
10. GitHub 六人协作规范
11. 推荐分工与责任矩阵
12. 一周里程碑与每日计划
13. 测试、质量门禁与 Definition of Done
14. 部署、演示与交付物清单
15. 风险、决策与待确认事项
附录 A：本地环境与端口表
附录 B：首批 API 清单
附录 C：项目启动检查表
使用方式  本文件是启动基线。任何跨服务接口、数据库字段、鉴权方式或版本变更，必须先更新 Issue/ADR，再修改代码和本文件。

# 1. 文档目标与项目成功标准
本项目是 JavaEE 企业级开发课程的一周集中考核项目。团队以企业协作方式交付前后端分离的分布式微服务 OA 系统，所有成员必须在代码、文档、测试和答辩中留下可核验贡献。本文将课程要求转化为可以直接执行的工程基线。
## 1.1 成功标准
全部基础必做功能可通过网关完成端到端演示，不能仅展示单服务接口或静态页面。
后端采用 Spring Boot 3 微服务体系，Nacos 同时承担注册与配置，JWT 实现无状态鉴权。
至少完成一项拓展功能；本方案将 AI 智能办公问答设为主扩展，Elasticsearch 全文检索设为次扩展。
六名成员各自拥有独立模块、Issue、提交记录、PR、测试与答辩讲解内容。
答辩前一次启动成功，演示环境、测试数据、录屏、PPT、SQL 和 README 齐全。
## 1.2 决策原则

# 2. 课程要求追踪与范围基线
## 2.1 硬性要求追踪矩阵

## 2.2 MVP 范围
用户与组织权限：登录、退出、部门/岗位/员工 CRUD、角色与菜单/接口权限分配。
考勤：上下班打卡、今日状态、个人记录、管理员查询；迟到阈值来自 Nacos。
审批：请假/加班提交、直属领导一级审批、待办/已办、同意/驳回。
公告：管理员发布；员工列表、详情、已读/未读状态。
公共能力：统一响应、全局异常、参数校验、操作日志、跨域、JWT、Redis、接口文档。
## 2.3 推荐扩展范围
主扩展：AI 智能办公问答  使用 Spring AI + Ollama + Redis Vector Store，将考勤规则、审批流程和人事制度做成可追溯的 RAG 问答；基础模型 qwen2.5:1.5b，嵌入模型 nomic-embed-text。
次扩展：Elasticsearch 检索  公告和审批标题/正文进入 Elasticsearch 8.18.1，支持关键词、高亮和条件过滤。若工期不足，只保留公告全文检索。
## 2.4 本周明确不做
Flowable 多级流程、Seata 分布式事务、XXL-Job、MQ、Sentinel、SkyWalking 不进入首周承诺范围。
不做生产级多租户、复杂刷新令牌、短信/邮件、移动端适配或 Kubernetes。
不为技术展示而拆出无业务价值的小服务；模块边界以可交付为准。
# 3. 技术基线与总体架构
## 3.1 建议锁定版本

版本规则  除修复明确漏洞外，开发周内禁止随意升级大/小版本。所有版本由父 POM 或 lockfile 单点管理，PR 中不得散落重复版本号。
## 3.2 逻辑架构

## 3.3 请求链路
浏览器携带 Bearer Token 请求 /api/v1/**。
Gateway 删除客户端伪造的 X-User-* 头，校验 JWT、黑名单和权限，生成 traceId。
Gateway 根据 Nacos 服务发现转发，并注入用户 ID、角色与权限上下文。
业务服务再次校验内部上下文，执行参数校验、事务和审计日志。
响应统一封装为 code/message/data/traceId/timestamp；异常不得直接暴露堆栈。
# 4. 后端仓库与 Maven 多模块结构
## 4.1 开发目录
D:\oa-system\
├─ oa-system-backend\       # GitHub: oa-system-backend
├─ oa-system-frontend\       # GitHub: oa-system-frontend（确认是否保留拼写）
├─ docs\                    # 团队级文档、PPT、录屏清单
└─ ops\                     # Compose、启动脚本、备份说明
## 4.2 后端仓库建议结构
oa-system-backend/
├─ pom.xml
├─ oa-common/
│  ├─ oa-common-core
│  ├─ oa-common-web
│  ├─ oa-common-security
│  └─ oa-common-redis
├─ oa-gateway
├─ oa-user-service
├─ oa-attendance-service
├─ oa-flow-service
├─ oa-notice-service
├─ oa-ai-service
├─ docs/
├─ sql/
├─ deploy/
└─ README.md
## 4.3 模块依赖规则
业务服务可以依赖 common，但 common 绝不能反向依赖任何业务服务。
业务服务之间不直接引用对方 entity/mapper；共享契约放在独立 API DTO 或通过 HTTP DTO 定义。
禁止把所有 DTO、枚举和工具类堆入 common；只有至少两个服务稳定复用的无业务代码才能下沉。
每个服务拥有独立 application.yml、启动类、数据库迁移/SQL、README 与测试。
# 5. 微服务职责与接口边界

## 5.1 跨服务协作
审批提交时，flow-service 通过 user-service 查询直属领导；保存单据与审批任务必须在本服务本地事务内完成。
公告/审批写入成功后再同步 Elasticsearch；同步失败记录补偿任务，不回滚核心业务。
AI 服务只读取经过审核的制度文档，不直接读取业务库敏感数据；若后续分析考勤，需调用脱敏统计 API。
服务超时默认 3 秒，重试只用于幂等查询；写操作不自动重试。
# 6. 数据模型与存储设计
## 6.1 MySQL 核心表

## 6.2 通用字段
id BIGINT PRIMARY KEY
created_by BIGINT
created_at DATETIME(3)
updated_by BIGINT
updated_at DATETIME(3)
deleted TINYINT DEFAULT 0
version INT DEFAULT 0
## 6.3 关键业务约束
考勤同一员工同一工作日最多一条记录，数据库唯一索引是最终幂等保障。
请假结束时间必须晚于开始时间；审批人不能审批自己提交的单据。
公告只有 PUBLISHED 状态对普通员工可见；已读记录使用唯一索引避免重复。
所有时间以 Asia/Shanghai 保存与展示；后端统一使用 LocalDate/LocalDateTime。
## 6.4 Elasticsearch 索引

# 7. 认证授权与安全设计
## 7.1 JWT 流程
登录：user-service 校验 BCrypt 密码，生成短期 Access Token；响应不返回密码、盐或内部状态。
鉴权：Gateway 校验签名、exp、issuer、jti 与 Redis 黑名单，再匹配接口权限。
退出：把 jti 写入 Redis 黑名单，TTL 等于 Token 剩余有效期，实现无状态退出。
上下文：Gateway 只注入 userId/username/roles/permissions；业务服务不信任客户端同名请求头。
## 7.2 Token 建议

## 7.3 权限模型
RBAC 采用用户—角色—菜单/接口权限。接口权限使用稳定字符串，例如 sys:user:list、attendance:record:query、flow:task:approve。前端按钮权限只改善体验，后端权限校验才是安全边界。
安全底线  JWT 密钥、数据库密码、Nacos 身份参数、GitHub Token 和任何模型 API Key 不得写入仓库。使用环境变量、本地 .env（加入 .gitignore）或 Nacos 加密配置。
# 8. 中间件与配置规范
## 8.1 Nacos

## 8.2 Redis Key 规范
oa:auth:blacklist:{jti}                 # TTL=Token 剩余时长
oa:user:permission:{userId}             # TTL=30min
oa:attendance:lock:{userId}:{yyyyMMdd}  # TTL=10s
oa:notice:unread:{userId}               # TTL=5min
spring-ai-index / embedding:*           # Spring AI 向量索引
## 8.3 分布式锁
考勤提交使用 Redis SET key token NX EX 获取锁，释放时用 Lua 脚本比较 token 后删除；数据库唯一索引仍是最终一致性防线。锁超时必须大于正常事务耗时，异常路径必须释放。
## 8.4 AI RAG
知识来源：教师/管理员审核后的考勤规则、审批流程、人事制度文档。
入库：切分文本→nomic-embed-text 生成 768 维向量→写入 Redis 索引。
问答：问题向量化→TopK 检索→拼接上下文→qwen2.5:1.5b 生成回答。
回答必须返回引用片段或来源标题；无命中时明确表示不知道，禁止编造制度。
本机 Ollama 因端口保留使用 http://localhost:12434，而非默认 11434。
# 9. API 与错误码规范
## 9.1 URL 与方法
统一前缀 /api/v1；资源名使用复数名词和 kebab-case。
GET 查询，POST 创建/业务动作，PUT 全量更新，PATCH 局部更新，DELETE 删除。
分页参数 page=1&size=20；排序使用 sort=createdAt,desc；最大 size=100。
时间使用 ISO-8601，例如 2026-07-20T09:30:00+08:00。
## 9.2 统一响应
{
  "code": "0",
  "message": "success",
  "data": {},
  "traceId": "01J...",
  "timestamp": "2026-07-20T10:00:00+08:00"
}
## 9.3 错误码

# 10. GitHub 六人协作规范
## 10.1 仓库

命名确认  仓库名 oa-system-fronted 中的 fronted 通常应为 frontend。如果尚无外部依赖，建议团队在第一天决定是否更名；若保留，所有文档和脚本必须使用同一拼写。
## 10.2 分支与 PR
main：稳定演示分支，禁止直接推送；develop：每日集成分支。
feature/<issue>-<short-name>、fix/<issue>-<short-name>、docs/<issue>-<short-name>。
所有变更通过 PR 合并，至少 1 人审查；模块负责人不能独自批准自己的关键变更。
默认 Squash Merge；PR 标题使用 Conventional Commits，如 feat(attendance): 支持上班打卡。
后端 CI 至少执行 mvn -B -ntp verify；前端 CI 执行 npm ci、lint、build。
## 10.3 提交与评审标准

# 11. 推荐分工与责任矩阵
以下为启动建议，姓名由团队 kickoff 时填写。每人必须有一个独立、可运行、可测试、可讲解的模块；测试与文档是所有人的共同责任，不应完全转移给某一人。

## 11.1 RACI 关键事项

# 12. 一周里程碑与每日计划

## 12.1 每日节奏
09:00：15 分钟站会，只说昨日完成、今日目标、阻塞。
14:00：接口契约/数据库变更同步，避免前后端漂移。
18:00：合并窗口，develop 必须可构建、可启动。
21:00：负责人更新燃尽清单、风险和次日 P0。
# 13. 测试、质量门禁与 Definition of Done
## 13.1 测试层次

## 13.2 PR 合并门禁
代码能编译；测试通过；无敏感信息；无未说明的 SQL/配置变更。
新增接口有 OpenAPI 描述、权限标识、参数校验和至少一个成功/失败测试。
跨服务或数据库变更已同步对应前端和 README。
日志不打印密码、完整 Token、身份证号等敏感数据。
至少一位非作者完成评审，关键架构变更由后端负责人批准。
## 13.3 Definition of Done
完成定义  功能只有在代码合并、测试通过、文档更新、接口可从网关调用、演示数据已准备、负责人可解释实现与风险后，才算完成。仅“本机跑通”不算完成。
# 14. 部署、演示与交付物清单
## 14.1 启停顺序
启动 Docker Desktop，确认 Nacos、Redis、Elasticsearch healthy/up。
启动 MySQL，执行/检查初始化 SQL。
启动 Ollama，确认 12434 API 和两个模型可用。
依次启动 user、attendance、flow、notice、ai，最后启动 gateway。
启动前端，执行 smoke test，再开始录屏或答辩。
## 14.2 交付物
源码、SQL、接口文档、启动脚本、录屏、PPT、演示账号、测试数据、README、版本说明与问题清单。
公告模块交付物应包含 notice 表结构、已读记录表、接口清单、权限说明、联调用例和至少一组演示数据。
## 14.3 答辩讲解顺序
目标与架构：为什么拆这些服务，Nacos、Gateway、JWT 如何协作。
基础业务：登录/RBAC→考勤→审批→公告完整链路。
分布式能力：Redis 锁、JWT 黑名单、动态配置和失败处理。
扩展能力：AI RAG 的检索、向量、模型和引用；可选 ES 检索。
分工证据：每人展示模块、关键代码、测试、PR 和技术难点。
# 15. 风险、决策与待确认事项
## 15.1 风险登记

## 15.2 启动会必须确认
六名成员姓名、GitHub 用户名、主责模块和备份人。
GitHub 仓库所属账号/组织、可见性、分支保护和成员权限。
是否将 oa-system-fronted 更名为 oa-system-frontend。
MySQL 数据库名、非 root 开发账号和密码分发方式。
JWT 密钥、Nacos 配置和 .env 的安全管理方式。
最终答辩日期、演示时长、录屏/PPT 格式和压缩包命名。
# 附录 A：本地环境与端口表

# 附录 B：首批 API 清单

# 附录 C：项目启动检查表
[ ] 六人姓名、GitHub 用户名和模块负责人已填写
[ ] 两个仓库成员权限和分支保护已启用
[ ] D:\oa-system 目录和仓库克隆完成
[ ] 父 POM、BOM 和 Java 21 构建通过
[ ] Compose 三个容器与 MySQL/Ollama 健康
[ ] SQL v1 与测试账号生成
[ ] OpenAPI 接口契约 v1 冻结
[ ] Day 0 Issue 全部建立并分配
[ ] 每日合并、测试、风险更新时间确定
[ ] 答辩演示脚本和交付物负责人确定
下一步  后端负责人依据本文创建 oa-system-backend 骨架、父 POM、common 与 gateway；前端负责人同步创建 Vue 3 基座。首个共同验收点是“登录请求经 Gateway 到 user-service 并返回 JWT”。

---

## 文档中的表格

### 表格 1
| 文档版本 | v1.0（项目启动基线） |
| --- | --- |
| 编制日期 | 2026-07-20 |
| 团队规模 | 6 人 |
| 后端负责人 | 待填写（当前文档发起人） |
| 代码仓库 | oa-system-backend / oa-system-fronted |
| 开发根目录 | D:\oa-system |

### 表格 2
| 优先级 | 原则 | 解释 |
| --- | --- | --- |
| P0 | 基础功能完整 | 先保证登录、RBAC、组织、考勤、审批、公告和公共能力闭环。 |
| P1 | 一周内可交付 | 优先复用现有环境与成熟 Starter，避免临时引入高学习成本组件。 |
| P2 | 边界清晰 | 每个服务只拥有自己的业务表和领域逻辑，跨服务通过 API 协作。 |
| P3 | 答辩可解释 | 每个技术选择都应能说明原理、取舍、失败处理和验证方式。 |

### 表格 3
| 课程要求 | 设计落点 | 验收证据 | 级别 |
| --- | --- | --- | --- |
| Spring Boot 3 + Spring Cloud Alibaba | 统一父 POM 与 BOM | effective-pom、构建日志 | 必做 |
| Nacos 注册/配置 | 全部服务注册；共享配置动态刷新 | 控制台实例、刷新演示 | 必做 |
| MySQL + MyBatis/Plus | 按服务逻辑分表；统一审计字段 | SQL、CRUD、事务测试 | 必做 |
| Redis 缓存/分布式锁 | 权限缓存、退出黑名单、考勤幂等锁 | Key 规范、并发测试 | 必做 |
| Gateway + JWT | 网关统一校验并注入可信用户上下文 | 非法/过期/越权用例 | 必做 |
| 四类业务服务 | 用户、考勤、审批、公告服务 | 端到端功能演示 | 必做 |
| 统一异常/返回/跨域/日志 | common-web、gateway、AOP 日志 | 接口错误与 traceId | 必做 |
| 至少一项扩展 | AI 智能问答 + RAG | 真实问答与知识检索 | 必做扩展 |
| 源码/SQL/文档/录屏/PPT | 仓库 docs 与 release 包 | 答辩前清单签字 | 交付 |

### 表格 4
| 层次 | 技术/版本 | 约束 |
| --- | --- | --- |
| 语言与构建 | JDK 21.0.4；Maven 3.9.16 | 全员统一；编译 release=21 |
| 应用框架 | Spring Boot 3.5.16 | 所有业务服务统一，不混用 Boot 2 |
| 微服务 | Spring Cloud 2025.0.0；SCA 2025.0.0.0 | 由 BOM 管理依赖 |
| 注册配置 | Nacos Server 3.2.2 | API 8848；控制台 8849 |
| 数据访问 | MySQL 8.0；MyBatis-Plus 3.5.x | 复杂 SQL 可使用 XML |
| 缓存/向量 | Redis 8.2.7 | 缓存、锁、JWT 黑名单、向量检索 |
| 检索 | Elasticsearch 8.18.1 | 使用 Spring Data 新客户端 |
| AI | Spring AI 1.1.8；Ollama 0.17.5 | Ollama 地址 localhost:12434 |
| 前端 | Vue 3 + Element Plus + Node 22 | 由前端仓库锁定 package-lock |

### 表格 5
| 层 | 组件 | 职责 |
| --- | --- | --- |
| 访问层 | Vue 3 SPA | 页面、路由、权限按钮、表单校验、可视化 |
| 入口层 | oa-gateway | 路由、CORS、JWT 校验、权限前置、traceId、限流预留 |
| 业务层 | user / attendance / flow / notice / ai | 领域业务、事务、数据权限、事件与索引同步 |
| 公共层 | oa-common-* | 响应、异常、日志、JWT、Redis、Feign 契约；禁止承载业务 |
| 基础设施 | Nacos / MySQL / Redis / ES / Ollama | 注册配置、数据、缓存向量、检索、模型推理 |

### 表格 6
| 服务 | 端口 | 核心职责 | 拥有数据 |
| --- | --- | --- | --- |
| oa-gateway | 8080 | 路由、JWT、权限、CORS、traceId | 无业务表 |
| oa-user-service | 8101 | 登录退出、员工、部门、岗位、RBAC | sys_* |
| oa-attendance-service | 8102 | 打卡、记录、异常判定、统计 | att_* |
| oa-flow-service | 8103 | 请假/加班、一级审批、待办已办 | flow_* |
| oa-notice-service | 8104 | 公告发布、上下线、已读未读 | notice_* |
| oa-ai-service | 8105 | 知识入库、RAG 问答、引用返回 | Redis 向量索引 |

### 表格 7
| 领域 | 建议表 | 关键字段/约束 |
| --- | --- | --- |
| 组织权限 | sys_user, sys_dept, sys_post, sys_role, sys_menu, sys_user_role, sys_role_menu | username 唯一；逻辑删除；角色/菜单多对多 |
| 考勤 | att_record, att_rule | user_id+work_date 唯一；check_in/out；status；version |
| 审批 | flow_request, flow_task, flow_action_log | request_no 唯一；type/status；approver_id；操作时间 |
| 公告 | notice, notice_read | publish_status；publisher_id；user_id+notice_id 唯一 |
| 审计 | sys_operation_log | trace_id、operator_id、module、action、result、duration |

### 表格 8
| 索引 | 内容 | 建议字段 |
| --- | --- | --- |
| oa_notice_v1 | 公告全文检索 | id, title(text), content(text), publisher, publishTime, status |
| oa_flow_v1 | 审批单据检索 | id, requestNo, type, applicantName, reason(text), status, createdAt |

### 表格 9
| 项目 | 建议 |
| --- | --- |
| 算法 | HS256（课程演示）或 RS256；密钥不得提交 Git |
| 有效期 | 2 小时；答辩演示足够，后续可加 Refresh Token |
| Claims | sub, userId, username, roles, permissions, jti, iat, exp, iss |
| 传输 | Authorization: Bearer <token>；生产必须 HTTPS |
| 密码 | BCrypt；登录失败统一提示，避免枚举账号 |

### 表格 10
| Data ID | 内容 | 动态刷新 |
| --- | --- | --- |
| oa-common.yaml | 日志、序列化、OpenAPI、公共超时 | 部分 |
| oa-gateway.yaml | 路由、白名单、CORS | 是 |
| oa-user-service.yaml | JWT issuer、登录策略 | 谨慎 |
| oa-attendance-service.yaml | 迟到阈值、锁 TTL、工作时间 | 是（答辩展示） |
| oa-flow-service.yaml | 审批规则、查询分页 | 是 |
| oa-ai-service.yaml | 模型名、Ollama 地址、TopK | 是 |

### 表格 11
| 范围 | 含义 | 示例 |
| --- | --- | --- |
| 0 | 成功 | 0 |
| A01xx | 认证失败 | A0101 凭证错误；A0102 Token 过期 |
| A02xx | 授权失败 | A0201 无接口权限 |
| B01xx | 参数/业务规则 | B0101 参数错误；B0110 重复打卡 |
| B02xx | 审批业务 | B0201 单据状态不可审批 |
| C01xx | 外部依赖 | C0101 Nacos；C0102 Redis；C0103 ES |
| S0001 | 未知系统异常 | 客户端只展示通用提示和 traceId |

### 表格 12
| 仓库 | 内容 | 默认负责人 |
| --- | --- | --- |
| oa-system-backend | Maven 多模块、SQL、后端 README、部署配置 | 后端负责人 |
| oa-system-frontend | Vue 3、页面、路由、API 封装、前端 README | 前端负责人 |

### 表格 13
| 类型 | 要求 |
| --- | --- |
| Issue | 需求、验收条件、负责人、预计完成日、关联模块 |
| Commit | 小步、可构建、信息清晰；禁止一次提交整个项目 |
| PR | 说明做了什么、怎么测试、接口/SQL变化、截图或调用示例 |
| Review | 关注边界、事务、权限、异常、并发、可测试性与敏感信息 |
| 证据 | 每人保留 Issue、Commit、PR、Review、测试记录和答辩讲解点 |

### 表格 14
| 成员 | 主责模块 | 必须交付 | 协作职责 |
| --- | --- | --- | --- |
| A 后端负责人 | 父工程、common、gateway、集成 | 骨架、JWT 网关、CI、总 README | 架构评审、合并、演示编排 |
| B 后端 | user-service | 登录退出、组织、员工、RBAC | 数据库基础数据、权限用例 |
| C 后端 | attendance-service | 打卡、记录、规则动态刷新、锁 | 并发测试、统计接口 |
| D 后端 | flow + notice | 请假/加班、审批、公告已读 | ES 同步或操作日志 |
| E 前端负责人 | 前端基座与权限 | 登录、布局、路由、菜单、组织权限页 | API 封装、UI 规范、构建 |
| F 前端/扩展 | 考勤审批公告 + AI 页面 | 业务页面、AI 对话或大屏 | 接口测试、录屏、PPT 汇总 |

### 表格 15
| 事项 | R 执行 | A 最终负责 | C 咨询 | I 知会 |
| --- | --- | --- | --- | --- |
| 技术基线/父 POM | A | A | B/C/D | 全员 |
| 数据库模型 | B/C/D | A | E/F | 全员 |
| 接口契约 | 对应前后端 | A/E | 测试伙伴 | 全员 |
| 每日集成 | A/E | A | 模块负责人 | 全员 |
| 录屏/PPT/答辩 | F + 全员 | A/E | 全员 | 全员 |

### 表格 16
| 时间 | 后端重点 | 前端重点 | 当日出口条件 |
| --- | --- | --- | --- |
| Day 0 | 父 POM、模块、Compose、Nacos、CI | 项目初始化、UI 基线 | 全员可启动；Issue/分工完成 |
| Day 1 | SQL v1、统一响应异常、用户/RBAC | 登录、布局、路由 | 登录经网关成功 |
| Day 2 | 考勤、审批骨架、接口文档 | 组织权限、考勤页 | 用户+考勤端到端 |
| Day 3 | 审批、公告、Redis 锁/黑名单 | 审批公告页 | 全部基础功能可走通 |
| Day 4 | AI RAG；ES 公告检索 | AI 对话/检索 UI | 至少一项扩展可演示 |
| Day 5 | 集成修复、测试、日志、SQL | 交互/UI/错误提示 | 功能冻结；P0/P1 缺陷清零 |
| Day 6 | 部署、README、数据重置 | 录屏、PPT、演示脚本 | 完整彩排 2 次 |
| 答辩日 | 提前启动并健康检查 | 页面和数据预热 | 不在现场等待下载/启动 |

### 表格 17
| 层次 | 范围 | 最低要求 |
| --- | --- | --- |
| 单元测试 | JWT、规则判断、审批状态机、Redis 锁 | 核心分支和边界值 |
| 数据测试 | Mapper、唯一索引、分页、事务 | H2 或测试库；SQL 可重复执行 |
| 接口测试 | 登录、CRUD、越权、重复提交、错误参数 | Postman/Apifox 集合可复跑 |
| 集成测试 | Gateway→服务→MySQL/Redis/Nacos | 基础主链路全部通过 |
| 演示回归 | 测试账号与演示脚本 | 连续两次完整无阻塞 |

### 表格 18
| 类别 | 内容 | 责任 |
| --- | --- | --- |
| 源代码 | 前后端仓库、README、版本标签 v1.0.0-demo | A/E |
| 数据库 | 建表、初始化、重置 SQL，数据字典 | B/C/D |
| 接口 | OpenAPI/Knife4j、测试集合、测试报告 | 各模块 |
| 部署 | Compose、启动脚本、环境变量模板、故障排查 | A |
| 演示 | 完整录屏、演示账号、演示脚本、PPT | F + 全员 |
| 贡献证明 | Issue/PR/Commit/Review/模块说明 | 每个人 |

### 表格 19
| 风险 | 概率/影响 | 预防与应对 |
| --- | --- | --- |
| Spring Boot/Cloud/Alibaba 版本冲突 | 中/高 | 先建最小骨架做依赖解析和启动测试；版本只在父 POM 管理 |
| 一周范围过大 | 高/高 | Day 3 基础功能冻结；扩展按 AI→ES 顺序降级 |
| 跨服务接口反复变更 | 高/中 | OpenAPI 契约先行；变更必须 Issue + 通知前端 |
| 成员贡献不均 | 中/高 | 每人独立模块和 PR；每日检查提交与出口条件 |
| 演示环境临场失败 | 中/高 | 离线镜像、初始化数据、健康检查、完整录屏兜底 |
| AI 回答不稳定 | 中/中 | 限定知识库、低温度、返回引用、准备固定演示问题 |

### 表格 20
| 组件 | 地址/端口 | 当前状态/备注 |
| --- | --- | --- |
| Gateway | http://localhost:8080 | 项目后续启动 |
| Nacos API | localhost:8848/nacos | Docker 已配置；gRPC 使用 9848/9849 |
| Nacos Console | http://localhost:8849 | 本地开发关闭认证 |
| MySQL | localhost:3306 | MySQL80 服务 |
| Redis | localhost:6379 | Redis 8.2.7，含向量检索 |
| Elasticsearch | http://localhost:9200 | 8.18.1，本地关闭安全 |
| Ollama | http://localhost:12434 | 默认 11434 被系统保留，已改端口 |
| Vue Dev Server | http://localhost:5173 | 前端启动后 |

### 表格 21
| 模块 | 方法与路径 | 说明/权限 |
| --- | --- | --- |
| 认证 | POST /api/v1/auth/login | 公开；签发 JWT |
| 认证 | POST /api/v1/auth/logout | 登录；加入黑名单 |
| 用户 | GET /api/v1/users/me | 当前用户资料与权限 |
| 组织 | GET/POST /api/v1/departments | sys:dept:list/create |
| 员工 | GET/POST/PUT /api/v1/users | sys:user:* |
| RBAC | PUT /api/v1/users/{id}/roles | sys:user:role |
| 考勤 | POST /api/v1/attendance/check-in | 员工打卡 |
| 考勤 | POST /api/v1/attendance/check-out | 员工打卡 |
| 考勤 | GET /api/v1/attendance/records | 本人/管理权限 |
| 审批 | POST /api/v1/flows/leave-requests | 提交请假 |
| 审批 | POST /api/v1/flows/overtime-requests | 提交加班 |
| 审批 | GET /api/v1/flows/tasks/todo | 当前审批人待办 |
| 审批 | POST /api/v1/flows/tasks/{id}/approve | 同意/驳回 |
| 公告 | POST /api/v1/notices | notice:create |
| 公告 | PUT /api/v1/notices/{id} | notice:update |
| 公告 | DELETE /api/v1/notices/{id} | notice:delete |
| 公告 | POST /api/v1/notices/{id}/publish | notice:publish |
| 公告 | POST /api/v1/notices/{id}/offline | notice:offline |
| 公告 | GET /api/v1/notices | 管理端公告列表 notice:list |
| 公告 | GET /api/v1/notices/{id} | 管理端公告详情 notice:view |
| 公告 | GET /api/v1/notices/public | 员工可见公告 |
| 公告 | GET /api/v1/notices/public/{id} | 员工公告详情 |
| 公告 | POST /api/v1/notices/{id}/read | 标记已读 |
| 公告 | GET /api/v1/notices/public/unread-count | 未读数量 |
| AI | POST /api/v1/ai/chat | SSE/普通响应，返回来源 |

### 通知公告模块最终版接口文档
| 项目 | 内容 |
| --- | --- |
| 模块目标 | 支持管理员发布公告、员工查看公告、记录已读状态，并在不拆分接口的前提下完成统一交付 |
| 核心表 | `notice`、`notice_read` |
| 状态枚举 | `DRAFT`、`PUBLISHED`、`OFFLINE` |
| 数据约束 | `notice_read.notice_id + user_id` 联合主键防重复；仅 `PUBLISHED` 状态对员工可见；公告支持摘要、置顶、下线时间、浏览统计与逻辑删除 |
| 统一响应 | `ApiResponse<{ code, message, data, traceId, timestamp }>` |
| 统一认证上下文 | 请求头 `X-User-Id`、`X-Trace-Id`、`X-Permissions`；后端不信任客户端自定义业务身份字段 |
| 管理端接口 | `POST /api/v1/notices` 创建公告；`PUT /api/v1/notices/{id}` 修改公告；`DELETE /api/v1/notices/{id}` 删除公告；`POST /api/v1/notices/{id}/publish` 发布公告；`POST /api/v1/notices/{id}/offline` 下线公告；`GET /api/v1/notices` 分页查询公告；`GET /api/v1/notices/{id}` 查询公告详情 |
| 员工端接口 | `GET /api/v1/notices/public` 分页查询已发布公告；`GET /api/v1/notices/public/{id}` 查询公告详情；`POST /api/v1/notices/{id}/read` 标记已读；`GET /api/v1/notices/public/unread-count` 查询未读数量 |
| 列表查询参数 | `keyword`、`status`、`topFlag`、`page`、`size`；默认按 `topFlag desc, publishedAt desc, updatedAt desc` 排序 |
| 权限标识 | `notice:create`、`notice:update`、`notice:delete`、`notice:publish`、`notice:offline`、`notice:list`、`notice:view` |
| 状态流转 | `DRAFT -> PUBLISHED`；`PUBLISHED -> OFFLINE`；`OFFLINE` 不可直接发布，需回到草稿后再操作；已发布公告不可直接删除 |
| 已读规则 | 单个用户对单个公告仅允许一条已读记录；重复标记已读应幂等 |
| 验收标准 | 能创建/发布/下线公告；能分页查询与查看详情；能标记已读并统计未读；权限与状态流转正确；接口测试通过 |
| 开发清单 | 1. 完成实体、DTO、VO、Enum；2. 完成 Mapper 与 SQL；3. 完成 Service 与状态流转；4. 完成 Controller 与权限控制；5. 完成单元测试与接口测试；6. 更新 SQL、文档与演示数据 |
| 备注 | 该模块当前采用单一 controller 方案，不拆分管理端/员工端 controller，前端按页面调用不同接口即可 |

### 通知公告模块前端 API 约定
| 项目 | 内容 |
| --- | --- |
| API 文件建议 | `src/api/notice.ts` 或按页面拆分为 `noticeAdmin.ts`、`noticePublic.ts` |
| 请求头 | 自动携带 `X-User-Id`、`X-Trace-Id`、`X-Permissions`；前端不直接拼接业务身份信息 |
| 管理端方法 | `createNotice`、`updateNotice`、`deleteNotice`、`publishNotice`、`offlineNotice`、`getNoticePage`、`getNoticeDetail` |
| 员工端方法 | `getPublicNoticePage`、`getPublicNoticeDetail`、`markNoticeRead`、`getUnreadNoticeCount` |
| 列表参数 | `keyword`、`status`、`topFlag`、`page`、`size` |
| 返回结构 | 直接读取 `ApiResponse.data`；列表为 `NoticePageVO<NoticeListItemVO>`，详情为 `NoticeDetailVO`，未读数为 `NoticeUnreadCountVO` |
| 页面映射 | 管理公告页、公告编辑页、公告详情页、员工公告页、员工公告详情页、未读角标组件 |
| 联调注意 | `public` 接口只展示员工可见公告；管理端接口必须传权限头；已读接口需要登录用户 ID |
| 交付要求 | 前端 API 命名与后端接口一致，页面和文档同步更新，不允许写死请求路径常量散落各处 |

### 通知公告模块开发收口清单
| 项目 | 是否完成 | 说明 |
| --- | --- | --- |
| 实体 / DTO / VO / Enum | 是 | 领域对象已就绪 |
| Mapper / SQL | 是 | 基础 CRUD 与已读统计完成 |
| Service / 状态流转 | 是 | 支持创建、修改、发布、下线、删除、已读、未读统计 |
| Controller / 权限 / 用户上下文 | 是 | 统一单 controller、权限头、用户头已接入 |
| 分页查询 | 是 | 支持 `page` / `size` / `keyword` / `status` / `topFlag` |
| 测试 | 部分完成 | 单测与接口测试已补，暂不要求集成测试 |
| 文档 | 是 | 已整理最终版接口文档和开发清单 |
| 前端 API | 待同步 | 按本节约定更新前端 API 文件即可 |

| 检索 | GET /api/v1/search/notices?q= | 公告全文检索 |

### AI 智能办公问答模块开发工作文档
| 项目 | 内容 |
| --- | --- |
| 模块目标 | 基于 Spring AI + Ollama + Redis Vector Store，实现对考勤规则、审批流程、人事制度的可追溯 RAG 问答，回答必须带来源片段或文档标题 |
| 技术栈 | Spring Boot 3.5.x、Spring AI 1.1.8、Ollama 0.17.5、Redis Stack/RediSearch、MyBatis-Plus、WebFlux/SSE（可选） |
| 模型选型 | LLM 使用 `qwen2.5:1.5b`，Embedding 使用 `nomic-embed-text`，Ollama 地址 `http://localhost:12434` |
| 知识范围 | 仅纳入已审核的制度文档：考勤规则、审批流程、人事制度、FAQ；不直接读取敏感业务库明细 |
| 核心能力 | 文档导入、文本切分、向量化入库、相似度检索、上下文拼接、生成回答、引用返回、无命中兜底 |
| 可追溯要求 | 每次回答返回 `sources`，至少包含 `title`、`chunkId`、`score`、`snippet`；若无法命中，明确提示“未找到相关制度依据” |
| 安全边界 | 不回答工资、身份证号、个人绩效、病历等敏感信息；不臆造制度；超出知识库范围时拒答并建议联系 HR 或管理员 |
| 接口形态 | `POST /api/v1/ai/chat`：普通 JSON 返回；`GET /api/v1/ai/chat/stream`：SSE 流式返回（可选） |
| 主要数据表 | `ai_knowledge_base`、`ai_knowledge_chunk`、`ai_chat_session`、`ai_chat_message`、`ai_ingest_job`（可按工期裁剪） |
| Redis 向量索引 | 使用 `spring-ai-index` 或自定义索引前缀保存 chunk 向量与元数据，支持 TopK 检索与过滤 |
| 切分策略 | 按标题/段落分块，chunk 大小建议 300~800 中文字，重叠 50~100 字，保留 `docId`、`section`、`orderNo` |
| 召回策略 | TopK=4~8，必要时叠加关键词过滤；优先召回命中的制度条款，再拼接回答上下文 |
| 提示词要求 | System Prompt 约束模型“只能依据上下文回答、必须引用来源、禁止编造、无法确认则说明不知道” |
| 质量标准 | 引用正确、回答简洁、术语一致、能命中至少一组演示问题、支持重复提问保持稳定 |
| 交付物 | 知识导入脚本、向量索引初始化脚本、AI 服务接口、测试数据、演示问题集、README 使用说明 |
| 验收用例 | 1. 询问迟到如何判定；2. 询问请假审批链路；3. 询问员工请假规则；4. 询问超出范围问题时能拒答 |
| 实施步骤 | 1. 完成制度文档整理与审核；2. 实现文档导入与切分；3. 实现向量写入与检索；4. 实现 RAG 提示模板；5. 实现问答接口；6. 补充测试与演示数据 |
| 备注 | 该模块优先保证“可追溯、可解释、可演示”，不追求复杂 Agent 或多轮记忆；多轮对话可在后续迭代增加 |
