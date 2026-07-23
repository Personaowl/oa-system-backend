# RBAC 接口说明

所有接口通过网关 `http://localhost:8080` 访问，并携带：

```http
Authorization: Bearer <accessToken>
```

网关验证 JWT 后注入用户、角色和权限上下文。业务服务使用 `PermissionGuard` 校验权限；拥有
`system:admin` 的用户可访问所有受保护接口。

## 管理接口

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/api/v1/roles` | `sys:role:list` |
| GET | `/api/v1/roles/{id}` | `sys:role:view` |
| POST | `/api/v1/roles` | `sys:role:create` |
| PUT | `/api/v1/roles/{id}` | `sys:role:update` |
| DELETE | `/api/v1/roles/{id}` | `sys:role:delete` |
| PUT | `/api/v1/roles/{id}/permissions` | `sys:role:assign-permission` |
| GET | `/api/v1/permissions` | `sys:permission:list` |
| GET | `/api/v1/users/{id}/roles` | `sys:user:role:list` |
| PUT | `/api/v1/users/{id}/roles` | `sys:user:assign-role` |
| GET | `/api/v1/users` | `sys:user:list` |
| POST | `/api/v1/users` | `sys:user:create` |
| PUT | `/api/v1/users/{id}` | `sys:user:update` |
| DELETE | `/api/v1/users/{id}` | `sys:user:delete` |

创建角色：

```json
{
  "code": "DEPT_MANAGER",
  "name": "部门管理员",
  "status": 1
}
```

分配权限或用户角色时，使用相同的请求结构。ID 推荐始终以字符串传输：

```json
{
  "ids": ["13", "14", "15"]
}
```

分配接口采用“整体替换”语义，传入空数组表示清空当前关联。

员工管理接口使用登录账号作为工号。创建员工时必须提供初始密码和至少一个角色；编辑时
`newPassword` 留空表示不修改密码。删除采用逻辑删除，且不能删除当前登录账号。

## 已接入业务权限

- 部门：`sys:dept:list/view/create/update/delete`
- 公告管理：`notice:list/view/create/update/delete/publish/offline`
- 员工公告：`notice:read`
- 用户/考勤/审批：`user:read`、`attendance:read`、`flow:read`
- AI 对话：`ai:chat`

## 生效说明

角色和权限在登录时写入 JWT。修改用户角色或角色权限后，旧 Token 不会立即变化，用户需要重新登录。
本地管理员首次使用前需要重新执行 `sql/02-seed-local-admin.sql`，再重新登录。
