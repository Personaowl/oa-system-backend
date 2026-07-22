# AI 智能办公问答模块前端 API 接口说明

版本：v1.0
日期：2026-07-22
适用范围：`oa-ai-service` 前端联调
接口风格：RESTful

## 1. 设计约定

### 1.1 基础约定

- 统一前缀：`/api/v1/ai`
- 请求方式遵循 RESTful 语义
- 列表查询使用 `GET`
- 资源创建使用 `POST`
- 资源更新使用 `PUT` 或 `PATCH`
- 删除使用 `DELETE`
- 详情查询使用 `GET /{id}`
- 所有接口默认返回 `ApiResponse<T>`
- 前端统一封装在 `src/api/ai.ts`、`src/api/aiKnowledge.ts`、`src/api/aiTask.ts`、`src/api/aiLog.ts`

### 1.2 通用请求头

- `Authorization: Bearer <token>`
- `X-Trace-Id: <traceId>`：可由网关注入，也可由前端透传用于调试
- `Content-Type: application/json`
- 上传接口使用 `multipart/form-data`

### 1.3 通用分页参数

- `page`：页码，从 1 开始
- `size`：每页条数，默认 20，最大 100
- `sort`：排序字段，如 `createdAt,desc`
- `keyword`：模糊搜索关键字

### 1.4 通用响应结构

```json
{
  "code": "0",
  "message": "success",
  "data": {},
  "traceId": "01J...",
  "timestamp": "2026-07-22T10:00:00+08:00"
}
```

---

## 2. 问答资源接口

### 2.1 发起问答

- 方法：`POST`
- 路径：`/api/v1/ai/chats`
- 说明：提交问题并获取基于 RAG 的回答

#### 请求体 `AiChatCreateDTO`

```json
{
  "question": "迟到多久算迟到？",
  "sessionId": 10001,
  "knowledgeDomain": "ATTENDANCE",
  "topK": 3,
  "stream": false
}
```

#### 字段说明

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| question | string | 是 | 用户问题 |
| sessionId | number | 否 | 会话 ID；为空时自动创建会话 |
| knowledgeDomain | string | 否 | 知识域：`ALL` / `ATTENDANCE` / `FLOW` / `HR` |
| topK | number | 否 | 召回条数，默认 3 |
| stream | boolean | 否 | 是否流式输出，默认 false |

#### 响应体 `AiChatResponseVO`

```json
{
  "sessionId": 10001,
  "answer": "根据考勤制度，迟到阈值为 9:15 之后。",
  "hitFlag": true,
  "matchedDocs": [
    {
      "docId": 1,
      "docTitle": "考勤管理制度",
      "docVersion": "v1.0"
    }
  ],
  "citations": [
    {
      "docId": 1,
      "docTitle": "考勤管理制度",
      "chunkId": 101,
      "chunkNo": 3,
      "snippet": "员工应于 9:15 之前完成上班打卡，超过该时间视为迟到。",
      "score": 0.92
    }
  ],
  "traceId": "01J..."
}
```

#### 前端封装建议

- 方法名：`chatAi`
- 用途：聊天窗口发送消息、获取答案

---

### 2.2 查询会话列表

- 方法：`GET`
- 路径：`/api/v1/ai/chat-sessions`
- 说明：查询当前登录用户的会话历史

#### 查询参数

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码，默认 1 |
| size | number | 否 | 每页条数，默认 20 |
| keyword | string | 否 | 按标题或首问模糊搜索 |
| status | string | 否 | `ACTIVE` / `ARCHIVED` |

#### 响应体 `PageResult<AiChatSessionVO>`

```json
{
  "list": [
    {
      "id": 1,
      "sessionNo": "CS202607220001",
      "sessionTitle": "迟到规则咨询",
      "knowledgeDomain": "ATTENDANCE",
      "latestQuestion": "迟到多久算迟到？",
      "latestAnswer": "根据考勤制度...",
      "messageCount": 4,
      "status": "ACTIVE",
      "createdAt": "2026-07-22T10:00:00+08:00"
    }
  ],
  "page": 1,
  "size": 20,
  "total": 1
}
```

#### 前端封装建议

- 方法名：`getChatSessionPage`

---

### 2.3 查询会话详情

- 方法：`GET`
- 路径：`/api/v1/ai/chat-sessions/{id}`
- 说明：查看某次会话的对话明细

#### 响应体 `AiChatSessionDetailVO`

```json
{
  "id": 1,
  "sessionNo": "CS202607220001",
  "sessionTitle": "迟到规则咨询",
  "knowledgeDomain": "ATTENDANCE",
  "status": "ACTIVE",
  "messages": [
    {
      "role": "USER",
      "content": "迟到多久算迟到？",
      "createdAt": "2026-07-22T10:00:00+08:00"
    },
    {
      "role": "ASSISTANT",
      "content": "根据考勤制度...",
      "citations": [
        {
          "docTitle": "考勤管理制度",
          "snippet": "员工应于 9:15 之前完成上班打卡..."
        }
      ],
      "createdAt": "2026-07-22T10:00:02+08:00"
    }
  ]
}
```

#### 前端封装建议

- 方法名：`getChatSessionDetail`

---

### 2.4 归档会话

- 方法：`PATCH`
- 路径：`/api/v1/ai/chat-sessions/{id}`
- 说明：将会话状态从 `ACTIVE` 改为 `ARCHIVED`

#### 请求体 `AiChatSessionUpdateDTO`

```json
{
  "status": "ARCHIVED"
}
```

#### 前端封装建议

- 方法名：`archiveChatSession`

---

### 2.5 删除会话

- 方法：`DELETE`
- 路径：`/api/v1/ai/chat-sessions/{id}`
- 说明：逻辑删除会话记录

#### 前端封装建议

- 方法名：`deleteChatSession`

---

## 3. 知识文档资源接口

### 3.1 上传知识文档

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs`
- 说明：上传制度文件，创建知识文档记录

#### 请求方式

- `multipart/form-data`

#### 表单字段 `AiKnowledgeDocCreateDTO`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | File | 是 | 制度文档文件 |
| docTitle | string | 是 | 文档标题 |
| docDomain | string | 是 | 知识域 |
| docVersion | string | 是 | 版本号 |
| effectiveDate | string | 否 | 生效日期，ISO-8601 或 yyyy-MM-dd |
| sourceType | string | 否 | 来源：`UPLOAD` / `MANUAL` / `SEED` |

#### 响应体 `AiKnowledgeDocVO`

```json
{
  "id": 1,
  "docTitle": "考勤管理制度",
  "docDomain": "ATTENDANCE",
  "docVersion": "v1.0",
  "fileName": "考勤管理制度v1.0.pdf",
  "fileUrl": "https://...",
  "status": "DRAFT",
  "effectiveDate": "2026-07-20",
  "createdAt": "2026-07-22T10:00:00+08:00"
}
```

#### 前端封装建议

- 方法名：`createKnowledgeDoc`

---

### 3.2 查询知识文档列表

- 方法：`GET`
- 路径：`/api/v1/ai/knowledge-docs`
- 说明：分页查询知识文档

#### 查询参数 `AiKnowledgeDocQueryDTO`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码 |
| size | number | 否 | 每页条数 |
| keyword | string | 否 | 关键字 |
| docDomain | string | 否 | 知识域 |
| status | string | 否 | 文档状态 |

#### 前端封装建议

- 方法名：`getKnowledgeDocPage`

---

### 3.3 查询知识文档详情

- 方法：`GET`
- 路径：`/api/v1/ai/knowledge-docs/{id}`
- 说明：查看文档详情和基础元数据

#### 响应体 `AiKnowledgeDocDetailVO`

```json
{
  "id": 1,
  "docTitle": "考勤管理制度",
  "docDomain": "ATTENDANCE",
  "docVersion": "v1.0",
  "fileName": "考勤管理制度v1.0.pdf",
  "fileUrl": "https://...",
  "contentHash": "abcdef...",
  "status": "APPROVED",
  "sourceType": "UPLOAD",
  "effectiveDate": "2026-07-20",
  "approvedBy": 1001,
  "approvedAt": "2026-07-22T10:05:00+08:00",
  "createdAt": "2026-07-22T10:00:00+08:00",
  "chunks": [
    {
      "id": 11,
      "chunkNo": 1,
      "chunkTitle": "打卡时间",
      "chunkText": "员工应于 9:15 之前完成上班打卡..."
    }
  ]
}
```

#### 前端封装建议

- 方法名：`getKnowledgeDocDetail`

---

### 3.4 更新知识文档

- 方法：`PUT`
- 路径：`/api/v1/ai/knowledge-docs/{id}`
- 说明：更新文档基础信息，不直接修改已入库正文

#### 请求体 `AiKnowledgeDocUpdateDTO`

```json
{
  "docTitle": "考勤管理制度（修订版）",
  "docDomain": "ATTENDANCE",
  "docVersion": "v1.1",
  "effectiveDate": "2026-07-23"
}
```

#### 前端封装建议

- 方法名：`updateKnowledgeDoc`

---

### 3.5 审核知识文档

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs/{id}/approve`
- 说明：通过审核后触发向量入库

#### 请求体

```json
{
  "remark": "制度内容已核对，允许入库。"
}
```

#### 前端封装建议

- 方法名：`approveKnowledgeDoc`

---

### 3.6 重新建立索引

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs/{id}/reindex`
- 说明：重新切分、向量化并写入 Redis Vector Store

#### 前端封装建议

- 方法名：`reindexKnowledgeDoc`

---

### 3.7 下线知识文档

- 方法：`POST`
- 路径：`/api/v1/ai/knowledge-docs/{id}/retire`
- 说明：停止该版本文档参与检索

#### 前端封装建议

- 方法名：`retireKnowledgeDoc`

---

### 3.8 删除知识文档

- 方法：`DELETE`
- 路径：`/api/v1/ai/knowledge-docs/{id}`
- 说明：逻辑删除知识文档

#### 前端封装建议

- 方法名：`deleteKnowledgeDoc`

---

## 4. 索引任务资源接口

### 4.1 查询索引任务列表

- 方法：`GET`
- 路径：`/api/v1/ai/index-tasks`
- 说明：查看文档入库、重建、删除索引任务

#### 查询参数 `AiIndexTaskQueryDTO`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码 |
| size | number | 否 | 每页条数 |
| status | string | 否 | 任务状态 |
| taskType | string | 否 | 任务类型 |
| docId | number | 否 | 文档 ID |

#### 前端封装建议

- 方法名：`getIndexTaskPage`

---

### 4.2 查询索引任务详情

- 方法：`GET`
- 路径：`/api/v1/ai/index-tasks/{id}`
- 说明：查看任务执行结果、错误信息与重试次数

#### 响应体 `AiIndexTaskVO`

```json
{
  "id": 1,
  "taskNo": "TASK202607220001",
  "docId": 1,
  "taskType": "INGEST",
  "status": "SUCCESS",
  "errorMessage": null,
  "retryCount": 0,
  "createdAt": "2026-07-22T10:00:00+08:00",
  "updatedAt": "2026-07-22T10:00:05+08:00"
}
```

#### 前端封装建议

- 方法名：`getIndexTaskDetail`

---

## 5. 问答日志资源接口

### 5.1 查询问答日志列表

- 方法：`GET`
- 路径：`/api/v1/ai/chat-logs`
- 说明：查询用户问答记录或管理员审计记录

#### 查询参数 `AiChatLogQueryDTO`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page | number | 否 | 页码 |
| size | number | 否 | 每页条数 |
| userId | number | 否 | 用户 ID |
| keyword | string | 否 | 问题或答案关键字 |
| knowledgeDomain | string | 否 | 知识域 |
| hitFlag | boolean | 否 | 是否命中 |

#### 前端封装建议

- 方法名：`getChatLogPage`

---

### 5.2 查询问答日志详情

- 方法：`GET`
- 路径：`/api/v1/ai/chat-logs/{id}`
- 说明：查看一次问答的完整引用、召回和生成结果

#### 响应体 `AiChatLogDetailVO`

```json
{
  "id": 1,
  "sessionId": 1,
  "userId": 1001,
  "question": "迟到多久算迟到？",
  "answer": "根据考勤制度...",
  "citations": [
    {
      "docId": 1,
      "docTitle": "考勤管理制度",
      "chunkId": 11,
      "snippet": "员工应于 9:15 之前完成上班打卡...",
      "score": 0.92
    }
  ],
  "modelName": "qwen2.5:1.5b",
  "topK": 3,
  "confidenceScore": 0.92,
  "hitFlag": true,
  "latencyMs": 1280,
  "createdAt": "2026-07-22T10:00:02+08:00"
}
```

#### 前端封装建议

- 方法名：`getChatLogDetail`

---

## 6. 前端 TypeScript 类型建议

### 6.1 问答类型

```ts
export interface AiChatCreateDTO {
  question: string;
  sessionId?: number;
  knowledgeDomain?: 'ALL' | 'ATTENDANCE' | 'FLOW' | 'HR';
  topK?: number;
  stream?: boolean;
}

export interface AiChatResponseVO {
  sessionId: number;
  answer: string;
  hitFlag: boolean;
  matchedDocs: Array<{
    docId: number;
    docTitle: string;
    docVersion: string;
  }>;
  citations: Array<{
    docId: number;
    docTitle: string;
    chunkId: number;
    chunkNo: number;
    snippet: string;
    score: number;
  }>;
  traceId: string;
}
```

### 6.2 知识文档类型

```ts
export interface AiKnowledgeDocCreateDTO {
  file: File;
  docTitle: string;
  docDomain: 'ATTENDANCE' | 'FLOW' | 'HR';
  docVersion: string;
  effectiveDate?: string;
  sourceType?: 'UPLOAD' | 'MANUAL' | 'SEED';
}

export interface AiKnowledgeDocVO {
  id: number;
  docTitle: string;
  docDomain: string;
  docVersion: string;
  fileName: string;
  fileUrl: string;
  status: 'DRAFT' | 'APPROVED' | 'RETIRED';
  effectiveDate?: string;
  createdAt: string;
}
```

### 6.3 日志类型

```ts
export interface AiChatLogVO {
  id: number;
  sessionId?: number;
  userId: number;
  question: string;
  answer: string;
  citations: Array<{
    docTitle: string;
    snippet: string;
    score?: number;
  }>;
  modelName: string;
  topK: number;
  confidenceScore?: number;
  hitFlag: boolean;
  latencyMs?: number;
  createdAt: string;
}
```

---

## 7. 接口文件建议

### 7.1 `src/api/ai.ts`

- `chatAi`
- `getChatSessionPage`
- `getChatSessionDetail`
- `archiveChatSession`
- `deleteChatSession`

### 7.2 `src/api/aiKnowledge.ts`

- `createKnowledgeDoc`
- `getKnowledgeDocPage`
- `getKnowledgeDocDetail`
- `updateKnowledgeDoc`
- `approveKnowledgeDoc`
- `reindexKnowledgeDoc`
- `retireKnowledgeDoc`
- `deleteKnowledgeDoc`

### 7.3 `src/api/aiTask.ts`

- `getIndexTaskPage`
- `getIndexTaskDetail`

### 7.4 `src/api/aiLog.ts`

- `getChatLogPage`
- `getChatLogDetail`

---

## 8. 联调注意事项

1. 问答接口必须带登录态。
2. 管理接口必须校验权限标识。
3. 上传接口需使用 `multipart/form-data`。
4. 前端展示引用内容时，建议支持“展开/收起”。
5. 列表分页默认后端返回 `page/size/total/list` 结构。
6. 若开启流式回答，前端需兼容 SSE 或分片渲染。
7. 文档状态与索引状态应分开展示，避免前端误解。

---

## 9. 推荐菜单权限

- `ai:chat`
- `ai:knowledge:list`
- `ai:knowledge:upload`
- `ai:knowledge:approve`
- `ai:knowledge:reindex`
- `ai:knowledge:retire`
- `ai:log:view`
- `ai:index:list`

---

## 10. 页面与接口对应关系

| 页面 | 主要接口 |
| --- | --- |
| AI 问答页 | `POST /api/v1/ai/chats`、`GET /api/v1/ai/chat-sessions`、`GET /api/v1/ai/chat-sessions/{id}` |
| 知识文档管理页 | `POST /api/v1/ai/knowledge-docs`、`GET /api/v1/ai/knowledge-docs`、`GET /api/v1/ai/knowledge-docs/{id}` |
| 文档详情页 | `GET /api/v1/ai/knowledge-docs/{id}`、`POST /api/v1/ai/knowledge-docs/{id}/approve`、`POST /api/v1/ai/knowledge-docs/{id}/reindex` |
| 索引任务页 | `GET /api/v1/ai/index-tasks`、`GET /api/v1/ai/index-tasks/{id}` |
| 问答日志页 | `GET /api/v1/ai/chat-logs`、`GET /api/v1/ai/chat-logs/{id}` |
