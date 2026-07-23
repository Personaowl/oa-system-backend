# 审批接口

所有接口统一经过 Gateway，除登录注册外均需携带 `Authorization: Bearer <token>`。
审批人访问待办、已办和审批接口时还必须拥有 `flow:task:approve` 权限。

## 状态与类型

- 申请类型：`LEAVE`（请假）、`OVERTIME`（加班）
- 申请状态：`PENDING`、`APPROVED`、`REJECTED`
- 审批决定：`APPROVE`、`REJECT`

## 提交申请

- `POST /api/v1/flows/leave-requests`
- `POST /api/v1/flows/overtime-requests`

```json
{
  "startTime": "2026-07-23T09:00:00",
  "endTime": "2026-07-23T18:00:00",
  "reason": "个人事务",
  "approverId": "2"
}
```

结束时间必须晚于开始时间，且审批人不能是申请人本人。

可通过 `GET /api/v1/flows/approvers` 获取当前账号可选择的启用审批人。该接口仅返回部门主管和系统管理员的基本信息，不要求员工拥有用户管理权限。

## 查询申请和任务

- `GET /api/v1/flows/requests/mine`：我的全部申请
- `GET /api/v1/flows/requests/{id}`：申请详情，仅申请人和相关审批人可见
- `GET /api/v1/flows/tasks/todo`：当前审批人的待办
- `GET /api/v1/flows/tasks/done`：当前审批人的已办

## 审批

`POST /api/v1/flows/tasks/{id}/approve`

```json
{
  "decision": "APPROVE",
  "comment": "同意"
}
```

驳回时将 `decision` 改为 `REJECT`。只有指定审批人可以处理任务，已处理任务不可重复审批。

已有数据库需要先执行 `sql/04-flow-approval-migration.sql`。
