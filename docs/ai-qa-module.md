# AI 智能办公问答模块开发文档

版本：v1.0
日期：2026-07-21
适用范围：OA 办公管理系统扩展模块
技术栈：Spring AI + Ollama + Redis Vector Store

## 1. 模块目标

本模块用于构建一个可追溯、可解释、可维护的企业办公知识问答能力，面向以下三类知识域提供检索增强生成（RAG）问答：

- 考勤规则
- 审批流程
- 人事制度

模块核心目标不是“自由聊天”，而是面向办公制度的可信问答。回答必须尽量基于知识库命中文本，并返回可核验来源；当知识库中不存在答案时，系统应明确说明“不知道”或“未检索到相关依据”，禁止编造制度内容。

## 2. 建设范围

### 2.1 本期范围

- 制度文档上传与入库
- 文档解析、切分、向量化
- Redis Vector Store 检索
- 基于 Ollama 的大模型生成回答
- 返回引用片段、来源标题、文档版本信息
- 问答记录留痕，支持后续审计与优化
- 后台知识库管理的基础能力

### 2.2 不在本期范围

- 多轮复杂智能体编排
- 流程自动执行与跨系统写操作
- 生产级权限分级审核流
- 多模型路由、A/B 测试、用户画像推荐
- 复杂文档 OCR 与图片理解
- 基于外部互联网的开放域搜索

## 3. 设计原则

1. 可追溯优先于“回答流畅”。
2. 宁可拒答，也不允许幻觉式编造。
3. 知识来源必须是经过管理员审核的制度文档。
4. 生成回答必须附带引用片段或来源说明。
5. 检索、生成、存储分层清晰，便于后续扩展。
6. AI 服务只做知识问答，不直接读取业务库敏感个人数据。

## 4. 总体架构

### 4.1 逻辑链路

1. 管理员上传考勤、审批、人事制度文档。
2. 系统解析文档内容并完成文本清洗。
3. 文档按规则切分为多个知识片段。
4. 使用 `nomic-embed-text` 生成向量。
5. 向量与元数据写入 Redis Vector Store。
6. 用户提问后，将问题转换为向量。
7. 向量检索 TopK 相关片段。
8. 将问题、检索结果、回答约束拼装为提示词。
9. 使用 `qwen2.5:1.5b` 生成回答。
10. 返回答案、引用片段、来源信息、命中情况。

### 4.2 组件职责

- `oa-ai-service`：AI 问答与知识入库服务
- `Ollama`：提供本地大模型推理与 embedding 能力
- `Redis Vector Store`：存储向量与元数据，承担知识召回
- `oa-user-service`：提供登录态与用户身份上下文
- `Nacos`：配置管理与服务发现
- `Gateway`：统一鉴权与请求入口

## 5. 技术选型

### 5.1 模型选型

- 聊天模型：`qwen2.5:1.5b`
- 嵌入模型：`nomic-embed-text`
- Ollama 地址：`http://localhost:12434`

### 5.2 存储选型

- 结构化元数据：MySQL（可选，用于文档目录、版本、审核状态、问答日志）
- 向量检索：Redis Vector Store
- 配置中心：Nacos

### 5.3 选型理由

- 本地 Ollama 便于演示和离线部署
- `qwen2.5:1.5b` 资源占用相对可控，适合课程项目
- Redis Vector Store 能复用现有 Redis 基础设施，部署简单
- Spring AI 降低模型接入与向量检索集成成本

## 6. 业务流程设计

### 6.1 知识入库流程

1. 管理员上传文档。
2. 系统校验文件类型、大小和内容范围。
3. 文档进入“待审核”状态。
4. 管理员确认知识可用后触发入库。
5. 系统提取文本、去噪、切分。
6. 每个片段生成 embedding。
7. 向量与元数据写入 Redis。
8. 记录文档版本、来源、更新时间与操作人。

### 6.2 问答流程

1. 用户输入问题。
2. 系统进行问题清洗与意图识别。
3. 将问题向量化。
4. 检索最相关的知识片段。
5. 根据检索结果拼接上下文。
6. 让模型基于上下文回答。
7. 输出答案、引用片段、来源标题、相似度信息。
8. 记录问答日志，便于复盘。

## 7. 文档与知识范围管理

### 7.1 知识域

模块初期仅覆盖以下内容：

- 考勤制度：迟到阈值、打卡规则、请假与补卡说明
- 审批流程：请假、加班、审批层级与状态定义
- 人事制度：入转调离、岗位职责、组织架构、权限说明

### 7.2 文档版本策略

每份制度文档都应具备：

- 文档标题
- 文档版本号
- 生效日期
- 审核状态
- 归属知识域
- 发布人
- 更新时间

系统只对“已审核”版本开放检索，避免草稿内容被回答引用。

## 8. 数据模型与表结构建议

本模块建议至少落三张 MySQL 业务表，Redis 用于向量索引与召回，不建议仅依赖 Redis 保存全部业务元数据。表结构以“文档 - 片段 - 问答记录”三层关系为主，便于追溯、审核和重建索引。

### 8.1 知识文档表 `ai_knowledge_doc`

用于维护制度文档的基础信息、版本、审核状态和文件地址。

**建议字段**

- `id`：主键
- `doc_title`：文档标题，如《考勤管理制度》
- `doc_domain`：知识域，`ATTENDANCE` / `FLOW` / `HR`
- `doc_version`：版本号，如 `v1.0`
- `file_name`：原始文件名
- `file_url`：文件存储地址
- `content_hash`：文档内容哈希，幂等控制
- `status`：`DRAFT` / `APPROVED` / `RETIRED`
- `source_type`：`UPLOAD` / `MANUAL` / `SEED`
- `effective_date`：生效日期
- `approved_by`：审核人
- `approved_at`：审核时间
- `created_by`
- `created_at`
- `updated_by`
- `updated_at`
- `deleted`

**关键约束**

- `content_hash` 建议唯一索引
- 同一知识域可存在多个版本，但只允许一个当前生效版本
- 仅 `APPROVED` 状态文档允许进入向量索引

### 8.2 知识片段表 `ai_knowledge_chunk`

用于存储切分后的片段元数据，支持追溯与重建向量索引。

**建议字段**

- `id`：主键
- `doc_id`：所属文档 ID
- `chunk_no`：片段序号，从 1 开始
- `chunk_title`：片段标题，可选
- `chunk_text`：片段正文
- `chunk_hash`：片段内容哈希
- `vector_key`：Redis 向量索引 key
- `metadata_json`：元数据快照
- `embedding_model`：向量模型名
- `embedding_dim`：向量维度
- `status`：`ACTIVE` / `INACTIVE`
- `created_at`

**关键约束**

- `doc_id + chunk_no` 建议建立唯一索引
- `chunk_hash` 用于检测重复切分
- 删除文档时建议逻辑失效片段，而不是直接物理删除，便于审计

### 8.3 问答会话表 `ai_chat_session`

用于保存用户的一次问答会话上下文，便于前端展示历史记录和后续多轮扩展。

**建议字段**

- `id`：主键
- `session_no`：会话编号
- `user_id`：提问人
- `session_title`：会话标题，可由首问生成
- `knowledge_domain`：当前会话知识域
- `latest_question`：最近一次问题
- `latest_answer`：最近一次答案摘要
- `message_count`：消息条数
- `status`：`ACTIVE` / `ARCHIVED`
- `created_at`
- `updated_at`

**关键约束**

- 同一用户可拥有多个会话
- 未来扩展多轮对话时以 `session_no` 作为上下文标识

### 8.4 问答记录表 `ai_chat_log`

用于保存每次提问、召回结果和最终回答，作为审计、回放和效果分析依据。

**建议字段**

- `id`：主键
- `session_id`：所属会话 ID
- `user_id`：提问用户
- `question`：用户问题
- `answer`：模型回答
- `retrieved_doc_ids`：命中文档 ID 列表
- `retrieved_chunk_ids`：命中片段 ID 列表
- `citations_json`：引用片段 JSON
- `model_name`：模型名称
- `top_k`：召回条数
- `confidence_score`：置信分
- `hit_flag`：是否命中知识库
- `latency_ms`：耗时
- `created_at`

**关键约束**

- 问答日志建议保留最近 90 天或按课程要求保留
- 不记录敏感个人隐私内容
- `session_id + created_at` 便于会话回放

### 8.5 索引重建任务表 `ai_index_task`

用于记录文档入库、重建索引、失败重试等异步任务。

**建议字段**

- `id`：主键
- `task_no`：任务编号
- `doc_id`：关联文档 ID
- `task_type`：`INGEST` / `REINDEX` / `DELETE_INDEX`
- `status`：`PENDING` / `RUNNING` / `SUCCESS` / `FAILED`
- `error_message`：失败原因
- `retry_count`：重试次数
- `created_by`
- `created_at`
- `updated_at`

### 8.6 关键约束

- 同一文档重复入库时需基于 `content_hash` 做幂等控制
- 片段切分后应保证 chunk 可回溯到原始文档
- 问答记录不得记录敏感原文之外的隐私信息
- 索引任务失败后应可重跑，不影响已发布文档的查询

## 9. Redis Vector Store 设计

### 9.1 索引目标

向量索引用于存储制度文本片段及其元数据，支持相似度检索。

### 9.2 元数据建议

每个向量片段至少保存：

- `docId`
- `docTitle`
- `docDomain`
- `docVersion`
- `chunkNo`
- `chunkText`
- `sourceFile`
- `approvedAt`
- `embeddingModel`

### 9.3 检索策略

- 默认 `TopK = 3` 或 `TopK = 5`
- 支持相似度阈值过滤
- 优先召回同知识域文档
- 对人事制度与审批流程问题可按标签做二次过滤

## 10. 切分与入库策略

### 10.1 文本切分原则

- 按标题、条款、段落切分
- 保留上下文连续性
- 避免切碎定义句、流程句和表格项
- 每个 chunk 以可回答一个局部问题为目标

### 10.2 建议切分参数

- chunk 大小：约 300～600 中文字符
- overlap：约 50～100 中文字符
- 对表格与制度条款优先按行拆分后再合并

### 10.3 清洗规则

- 去除多余空白、页码、页眉页脚
- 合并断行
- 统一全角半角与标点风格
- 保留条款编号，如“第3条”“3.2”“（一）”

## 11. 提示词与回答约束

### 11.1 系统提示词要求

提示词必须强调：

- 只允许基于检索上下文回答
- 不得虚构制度、时间、规则、审批链路
- 无依据时要明确拒答
- 优先给出条款式总结
- 必须输出引用来源

### 11.2 建议输出格式

回答建议包含：

- 直接结论
- 引用依据
- 来源文档
- 相关条款片段
- 不确定性说明（如有）

### 11.3 拒答策略

当出现以下情况时应拒答或降级：

- 检索结果为空
- 检索结果相似度过低
- 问题超出知识域
- 文档版本未审核
- 上下文冲突无法判断

拒答示例：

“未在已审核的考勤/审批/人事制度中检索到可确认依据，建议联系管理员确认最新制度版本。”

## 12. 前端 API 接口清单

本节面向前端联调用，建议统一封装在 `src/api/ai.ts`，若页面拆分较多，也可按“知识库管理 / 问答 / 日志”拆成多个文件。

### 12.1 问答接口

#### 1）发送问答请求

- 方法：`POST`
- 路径：`/api/v1/ai/chat`
- 用途：提交用户问题，返回基于 RAG 的答案

**请求参数**

- `question`：问题内容，必填
- `sessionId`：会话 ID，可选
- `knowledgeDomain`：知识域，`ATTENDANCE` / `FLOW` / `HR` / `ALL`
- `topK`：召回条数，默认 3
- `stream`：是否流式返回，前端可选

**返回内容**

- `answer`：模型回答
- `citations`：引用列表
- `hitFlag`：是否命中知识库
- `matchedDocs`：命中文档摘要
- `traceId`：链路追踪 ID
- `sessionId`：会话 ID

#### 2）获取问答历史

- 方法：`GET`
- 路径：`/api/v1/ai/chat-sessions`
- 用途：查询当前用户的会话列表

**查询参数**

- `page`
- `size`
- `keyword`：可按标题或首问搜索

#### 3）获取会话详情

- 方法：`GET`
- 路径：`/api/v1/ai/chat-sessions/{id}`
- 用途：查看某次会话的问答记录明细

### 12.2 知识管理接口

#### 1）上传知识文档

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs`
- 用途：上传制度文档并创建知识记录

**请求方式建议**

- `multipart/form-data`

**字段建议**

- `file`：制度文件
- `docTitle`：文档标题
- `docDomain`：知识域
- `docVersion`：版本号
- `effectiveDate`：生效日期
- `sourceType`：来源类型

#### 2）查询知识文档列表

- 方法：`GET`
- 路径：`/api/v1/ai/knowledge-docs`
- 用途：分页查询知识文档

**查询参数**

- `keyword`
- `docDomain`
- `status`
- `page`
- `size`

#### 3）查询知识文档详情

- 方法：`GET`
- 路径：`/api/v1/ai/knowledge-docs/{id}`
- 用途：查看文档详情、版本和审核状态

#### 4）审核知识文档

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs/{id}/approve`
- 用途：将文档从待审核切换为已审核，并触发入库

#### 5）重建索引

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs/{id}/reindex`
- 用途：重新切分并写入 Redis 向量索引

#### 6）下线知识文档

- 方法：`POST` 或 `DELETE` 均可按团队约定
- 路径：`/api/v1/ai/knowledge-docs/{id}/retire`
- 用途：将已发布文档下线，停止参与检索

### 12.3 索引与任务接口

#### 1）查询索引任务列表

- 方法：`GET`
- 路径：`/api/v1/ai/index-tasks`
- 用途：查看入库、重建、删除索引任务状态

**查询参数**

- `status`
- `taskType`
- `docId`
- `page`
- `size`

#### 2）查询索引任务详情

- 方法：`GET`
- 路径：`/api/v1/ai/index-tasks/{id}`
- 用途：查看任务错误信息和重试情况

### 12.4 问答日志接口

#### 1）查询问答日志列表

- 方法：`GET`
- 路径：`/api/v1/ai/chat-logs`
- 用途：查询问答记录

**查询参数**

- `userId`
- `keyword`
- `hitFlag`
- `knowledgeDomain`
- `page`
- `size`

#### 2）查询问答日志详情

- 方法：`GET`
- 路径：`/api/v1/ai/chat-logs/{id}`
- 用途：查看某次回答的引用片段、召回结果和耗时

### 12.5 前端 API 文件建议

- `src/api/ai.ts`：问答 + 历史 + 日志
- `src/api/aiKnowledge.ts`：知识文档管理
- `src/api/aiTask.ts`：索引任务管理

### 12.6 返回结构建议

前端统一读取 `ApiResponse.data`，建议具体 DTO 如下：

- `AiChatResponseVO`
- `AiChatSessionVO`
- `AiChatLogVO`
- `AiKnowledgeDocVO`
- `AiKnowledgeChunkVO`
- `AiIndexTaskVO`

### 12.7 联调用注意事项

- 问答接口必须携带登录态
- 管理接口必须校验权限标识
- `public` 类接口不存在于本模块；AI 模块默认仅供登录用户使用
- 引用列表建议前端可点击展开，查看来源标题与原文片段

## 13. 权限设计

## 13. 权限设计

### 13.1 角色划分

- 管理员：管理知识文档、触发入库、查看日志
- 普通员工：仅允许提问与查看自己的问答记录
- 审核人：确认文档可入库、可检索

### 13.2 权限标识建议

- `ai:chat`
- `ai:knowledge:list`
- `ai:knowledge:upload`
- `ai:knowledge:approve`
- `ai:knowledge:reindex`
- `ai:log:view`

## 14. 安全与合规

### 14.1 安全要求

- 不得把 JWT 密钥、模型密钥、数据库密码写入仓库
- 不得让模型直接读取未脱敏的敏感业务数据
- 问答日志避免记录身份证号、手机号等敏感字段
- 仅对审核后的制度内容开放向量检索

### 14.2 幻觉控制

- 限定问题域
- 限定上下文来源
- 限定回答长度与风格
- 对高风险问题增加拒答阈值

## 15. 配置建议

### 15.1 `application.yml` 关键项

- `spring.ai.ollama.base-url=http://localhost:12434`
- `spring.ai.ollama.chat.model=qwen2.5:1.5b`
- `spring.ai.ollama.embedding.model=nomic-embed-text`
- `ai.rag.top-k=3`
- `ai.rag.min-score=0.75`
- `ai.rag.max-chunk-size=600`
- `ai.rag.chunk-overlap=80`

### 15.2 环境要求

- Ollama 已启动并拉取模型
- Redis 已启用向量能力或兼容实现
- Nacos 配置已发布
- 网关已完成鉴权与上下文透传

## 16. 测试方案

### 16.1 单元测试

- 文档切分逻辑
- 问题意图与知识域判定
- 相似度阈值过滤
- 拒答逻辑
- 引用生成逻辑

### 16.2 集成测试

- 文档上传 → 审核 → 入库 → 检索 → 回答
- Redis Vector Store 可用性
- Ollama 推理可用性
- Gateway 鉴权后上下文透传

### 16.3 典型测试用例

- “迟到多久算迟到？”
- “请假审批要经过谁？”
- “入职后多久可以转正？”
- “这个制度有没有最新版本？”
- “如果知识库没有答案会怎么处理？”

## 17. 验收标准

模块验收通过需满足：

1. 能成功导入至少 3 份审核通过的制度文档。
2. 能对考勤、审批、人事三个知识域进行问答。
3. 回答可返回引用片段和来源标题。
4. 未命中时能明确拒答，不编造答案。
5. 问答链路可通过网关正常访问。
6. 至少具备基础日志留痕能力。
7. 能在答辩时演示一次完整 RAG 问答链路。

## 18. 开发任务拆分

### 18.1 后端任务

- 搭建 `oa-ai-service`
- 接入 Spring AI 与 Ollama
- 设计文档与日志表
- 完成知识导入、切分、向量化、检索、回答
- 完成接口与权限控制
- 编写单元测试与集成测试

### 18.2 数据任务

- 准备制度文档样例
- 设计初始化 SQL
- 准备演示数据和测试问题集

### 18.3 前端任务

- 增加 AI 问答页面
- 展示回答内容与引用来源
- 支持知识域筛选与历史记录查看

## 19. 风险与应对

| 风险 | 影响 | 应对 |
| --- | --- | --- |
| 模型回答不稳定 | 高 | 限定知识域、降低温度、强制引用 |
| 检索命中率低 | 中 | 优化切分粒度、TopK 和元数据过滤 |
| 文档内容不规范 | 中 | 统一模板与审核流程 |
| Redis 向量能力配置不一致 | 中 | 提前完成本地联调与健康检查 |
| 生成内容出现幻觉 | 高 | 明确拒答策略与上下文约束 |

## 20. 交付清单

- AI 服务源码
- 文档入库脚本或管理接口
- Redis Vector Store 配置说明
- Ollama 模型拉取说明
- 测试用制度文档
- 问答演示脚本
- 接口文档
- 单元测试与集成测试
- 答辩展示用问答样例

## 21. 后续演进方向

- 增加公告、制度全文与 FAQ 的统一知识库
- 增加问答会话记忆与多轮追问
- 接入 Elasticsearch 做结构化检索补充
- 增加知识审核工作流
- 支持多模型切换与性能观测

## 22. 附录：建议演示问题

1. “迟到阈值是多少？”
2. “请假审批需要几级审批？”
3. “转正制度怎么规定？”
4. “如果制度没有明确说明，系统会怎么回答？”
5. “请给出你回答的来源依据。”

---

文档说明：
本文件作为独立开发文档使用，可与主项目总文档并行维护。若后续 AI 模块范围扩大，应优先更新本文件，再同步回总文档。

## 23. 数据库建表 SQL

以下 SQL 已整理到独立脚本 `sql/03-ai-knowledge.sql`，建议由 `oa-ai-service` 单独维护并在初始化脚本中执行。
## 24. 后端 Controller / DTO / VO 清单

### 24.1 Controller 清单

建议在 `oa-ai-service` 中按职责拆分为以下控制器：

- `AiChatController`
  - `POST /api/v1/ai/chat`
  - `GET /api/v1/ai/chat-sessions`
  - `GET /api/v1/ai/chat-sessions/{id}`

- `AiKnowledgeDocController`
  - `POST /api/v1/ai/knowledge-docs`
  - `GET /api/v1/ai/knowledge-docs`
  - `GET /api/v1/ai/knowledge-docs/{id}`
  - `POST /api/v1/ai/knowledge-docs/{id}/approve`
  - `POST /api/v1/ai/knowledge-docs/{id}/reindex`
  - `POST /api/v1/ai/knowledge-docs/{id}/retire`

- `AiIndexTaskController`
  - `GET /api/v1/ai/index-tasks`
  - `GET /api/v1/ai/index-tasks/{id}`

- `AiChatLogController`
  - `GET /api/v1/ai/chat-logs`
  - `GET /api/v1/ai/chat-logs/{id}`

### 24.2 DTO 清单

#### 问答相关 DTO

- `AiChatRequestDTO`
  - `question`
  - `sessionId`
  - `knowledgeDomain`
  - `topK`
  - `stream`

- `AiChatSessionQueryDTO`
  - `page`
  - `size`
  - `keyword`

- `AiChatLogQueryDTO`
  - `page`
  - `size`
  - `userId`
  - `keyword`
  - `knowledgeDomain`
  - `hitFlag`

#### 知识文档 DTO

- `AiKnowledgeDocCreateDTO`
  - `docTitle`
  - `docDomain`
  - `docVersion`
  - `effectiveDate`
  - `sourceType`
  - `file`

- `AiKnowledgeDocQueryDTO`
  - `page`
  - `size`
  - `keyword`
  - `docDomain`
  - `status`

- `AiKnowledgeDocUpdateDTO`
  - `docTitle`
  - `docDomain`
  - `docVersion`
  - `effectiveDate`

#### 索引任务 DTO

- `AiIndexTaskQueryDTO`
  - `page`
  - `size`
  - `status`
  - `taskType`
  - `docId`

### 24.3 VO 清单

#### 问答 VO

- `AiChatResponseVO`
  - `answer`
  - `citations`
  - `hitFlag`
  - `matchedDocs`
  - `traceId`
  - `sessionId`

- `AiChatSessionVO`
  - `id`
  - `sessionNo`
  - `sessionTitle`
  - `knowledgeDomain`
  - `latestQuestion`
  - `latestAnswer`
  - `messageCount`
  - `status`
  - `createdAt`

- `AiChatLogVO`
  - `id`
  - `sessionId`
  - `question`
  - `answer`
  - `citations`
  - `modelName`
  - `topK`
  - `confidenceScore`
  - `hitFlag`
  - `latencyMs`
  - `createdAt`

#### 知识文档 VO

- `AiKnowledgeDocVO`
  - `id`
  - `docTitle`
  - `docDomain`
  - `docVersion`
  - `fileName`
  - `fileUrl`
  - `status`
  - `effectiveDate`
  - `approvedBy`
  - `approvedAt`
  - `createdAt`
  - `updatedAt`

- `AiKnowledgeChunkVO`
  - `id`
  - `docId`
  - `chunkNo`
  - `chunkTitle`
  - `chunkText`
  - `status`
  - `createdAt`

#### 索引任务 VO

- `AiIndexTaskVO`
  - `id`
  - `taskNo`
  - `docId`
  - `taskType`
  - `status`
  - `errorMessage`
  - `retryCount`
  - `createdAt`
  - `updatedAt`

### 24.4 推荐 Service 拆分

- `AiChatService`
  - 处理提问、检索、提示词拼装、生成回答、保存记录

- `AiKnowledgeDocService`
  - 处理文档创建、审核、下线、重建索引

- `AiIndexTaskService`
  - 处理异步任务查询与任务状态流转

- `AiVectorStoreService`
  - 封装 Redis 向量写入与召回

- `AiEmbeddingService`
  - 封装 embedding 生成

### 24.5 后端返回建议

统一返回 `ApiResponse<T>`，错误时保留 `traceId`，便于前端和日志联动。

## 25. 前端页面与路由设计

### 25.1 页面结构

建议 AI 模块前端至少包含以下页面：

1. **AI 问答首页**
   - 侧边栏展示历史会话
   - 主区域展示对话内容
   - 底部输入问题与发送按钮
   - 支持知识域选择和 TopK 调整

2. **知识文档管理页**
   - 文档列表
   - 文档上传
   - 文档审核
   - 文档下线
   - 文档重建索引

3. **问答日志页**
   - 问题列表
   - 命中情况
   - 耗时
   - 引用来源

4. **索引任务页**
   - 任务状态
   - 失败原因
   - 重试记录

### 25.2 路由设计

建议路由如下：

- `/ai/chat`
- `/ai/knowledge-docs`
- `/ai/chat-logs`
- `/ai/index-tasks`

如果项目采用菜单权限控制，可按如下菜单项配置：

- `AI 智能问答`
- `知识文档管理`
- `问答日志`
- `索引任务`

### 25.3 页面交互建议

- 问答页支持“发送后立即显示加载中”
- 回答中引用片段可折叠查看
- 知识文档上传后展示入库状态
- 审核通过后可直接触发重建索引
- 日志页支持按命中状态和知识域筛选

### 25.4 前端 API 文件建议

- `src/api/ai.ts`
  - `chat`
  - `getChatSessionPage`
  - `getChatSessionDetail`
  - `getChatLogPage`
  - `getChatLogDetail`

- `src/api/aiKnowledge.ts`
  - `createKnowledgeDoc`
  - `getKnowledgeDocPage`
  - `getKnowledgeDocDetail`
  - `approveKnowledgeDoc`
  - `reindexKnowledgeDoc`
  - `retireKnowledgeDoc`

- `src/api/aiTask.ts`
  - `getIndexTaskPage`
  - `getIndexTaskDetail`

## 26. `oa-ai-service` 代码骨架建议

### 26.1 推荐目录结构

```text
oa-ai-service/
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/personaowl/oa/ai/
│  │  │     ├─ AiServiceApplication.java
│  │  │     ├─ api/
│  │  │     │  ├─ AiChatController.java
│  │  │     │  ├─ AiKnowledgeDocController.java
│  │  │     │  ├─ AiIndexTaskController.java
│  │  │     │  └─ AiChatLogController.java
│  │  │     ├─ application/
│  │  │     │  ├─ service/
│  │  │     │  └─ dto/
│  │  │     ├─ domain/
│  │  │     │  ├─ entity/
│  │  │     │  ├─ vo/
│  │  │     │  └─ enums/
│  │  │     ├─ infrastructure/
│  │  │     │  ├─ config/
│  │  │     │  ├─ embedding/
│  │  │     │  ├─ vector/
│  │  │     │  └─ repository/
│  │  │     └─ common/
│  │  └─ resources/
│  │     ├─ application.yml
│  │     └─ db/migration/
└─ pom.xml
```

### 26.2 分层职责

- `api`
  - 只负责参数接收、权限注解、返回封装
- `application`
  - 负责编排业务流程
- `domain`
  - 放实体、枚举、值对象、核心规则
- `infrastructure`
  - 负责数据库、Redis、Ollama、向量检索、配置

### 26.3 核心启动类

- `AiServiceApplication`

### 26.4 关键配置类

- `OllamaClientConfig`
- `SpringAiConfig`
- `RedisVectorStoreConfig`
- `AiProperties`
- `AiWebMvcConfig`
- `AiSecurityConfig`（如与 common-security 集成）

### 26.5 启动后最小可验收链路

1. 服务启动成功
2. 文档上传成功
3. 文档审核成功
4. 索引生成成功
5. 提问成功并返回引用
6. 日志成功落库

## 27. 建议交付顺序

1. 先建表与实体
2. 再做知识文档上传和审核
3. 再做向量入库与问答接口
4. 再做前端问答页
5. 最后补日志、任务、测试和演示数据
联调可直接用的接口清单，包括：

POST /api/v1/ai/chat
GET /api/v1/ai/chat-sessions
GET /api/v1/ai/chat-sessions/{id}
POST /api/v1/ai/knowledge-docs
GET /api/v1/ai/knowledge-docs
GET /api/v1/ai/knowledge-docs/{id}
POST /api/v1/ai/knowledge-docs/{id}/approve
POST /api/v1/ai/knowledge-docs/{id}/reindex
POST /api/v1/ai/knowledge-docs/{id}/retire
GET /api/v1/ai/index-tasks
GET /api/v1/ai/index-tasks/{id}
GET /api/v1/ai/chat-logs
GET /api/v1/ai/chat-logs/{id}