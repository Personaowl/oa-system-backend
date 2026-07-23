# 审批接口

所有接口统一经过 Gateway，除登录注册外均需携带 `Authorization: Bearer <token>`。
审批人访问待办、已办和审批接口时还必须拥有 `flow:task:approve` 权限。

## 状态与类型

- 申请类型：`LEAVE`（请假）、`OVERTIME`（加班）
- 申请状态：`PENDING`、`APPROVED`、`REJECTED`、`WITHDRAWN`
- 请假类型：`PERSONAL`（事假）、`SICK`（病假）、`ANNUAL`（年假）、`COMPENSATORY`（调休）
- 加班补偿：`PAY`（加班费）、`COMPENSATORY`（调休）
- 流转动作：`SUBMIT`、`APPROVE`、`REJECT`、`WITHDRAW`

## 提交申请

- `POST /api/v1/flows/leave-requests`
- `POST /api/v1/flows/overtime-requests`

请假示例：

```json
{
  "startTime": "2026-07-23T09:00:00",
  "endTime": "2026-07-23T18:00:00",
  "reason": "个人事务",
  "leaveType": "PERSONAL"
}
```

加班示例：

```json
{
  "startTime": "2026-07-23T18:30:00",
  "endTime": "2026-07-23T21:30:00",
  "reason": "版本上线",
  "overtimeCompensation": "COMPENSATORY"
}
```

审批人不再由申请人手动选择。系统优先使用申请人所在部门的负责人，未配置时依次选择其他启用的系统管理员、部门主管。相同时间段不能重复提交待审批或已通过的申请。

`GET /api/v1/flows/approvers` 保留用于兼容旧客户端，只返回系统为当前用户自动确定的审批人。

## 查询申请和任务

- `GET /api/v1/flows/requests/mine`：我的全部申请
- `GET /api/v1/flows/requests/{id}`：申请详情及完整流转时间线，仅申请人和相关审批人可见
- `GET /api/v1/flows/tasks/todo`：当前审批人的待办
- `GET /api/v1/flows/tasks/done`：当前审批人的已办

详情返回结构中的 `request` 为申请数据，`timeline` 按发生时间升序包含提交、审批和撤回动作。

## 撤回

`POST /api/v1/flows/requests/{id}/withdraw`

只有申请人本人可以撤回 `PENDING` 状态的申请。撤回后状态变为 `WITHDRAWN`，原审批人待办会自动消失。

## 审批

`POST /api/v1/flows/tasks/{id}/approve`

```json
{
  "decision": "APPROVE",
  "comment": "同意"
}
```

驳回时将 `decision` 改为 `REJECT`，并且 `comment` 必填。只有指定审批人可以处理任务，已处理任务不可重复审批。

请假审批通过后，系统会为工作日写入 `LEAVE` 考勤记录，员工不会被统计为缺卡或异常。加班审批不会覆盖当天实际打卡记录。

## 数据库迁移

已有数据库按顺序执行：

1. `sql/04-flow-approval-migration.sql`
2. `sql/11-flow-enterprise-enhancement.sql`

`11-flow-enterprise-enhancement.sql` 可重复执行，并会为旧申请补齐分类、时长和提交时间线。
