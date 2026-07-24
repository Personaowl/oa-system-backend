# oa-asset-service

资产管理微服务，默认端口 `8107`，通过 Nacos 注册为 `oa-asset-service`。

主要能力：

- 办公用品库存维护、库存预警、员工申领、部门审批与确认发放
- 固定资产登记、分配、归还、维修/报废状态维护
- ADMIN/HR 查看全部数据；MANAGER 查看并审批负责部门；EMPLOYEE 查看本人数据

首次运行前执行：

```sql
source sql/17-asset-management.sql;
```

然后启动 `AssetServiceApplication`，网关会将 `/api/v1/assets/**` 转发到本服务。
