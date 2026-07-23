# OA Document Service

部门共享文档服务，默认端口为 `8106`。

## 权限范围

- 普通员工：查看和编辑所属部门的文档。
- 部门主管：除所属部门外，还可访问其负责部门；可以创建和删除文档。
- 超级管理员：访问和管理全部部门文档。

文档更新需要携带当前 `version`。服务使用乐观锁避免旧页面覆盖其他成员已经保存的新版本。

## 初始化

```powershell
mysql -u root -p oa_system
source D:/oa-system/backend/sql/16-shared-document.sql
```

## 启动

```powershell
mvn -pl oa-document-service spring-boot:run
```

启动或重启 `oa-gateway` 后，可以通过以下路径访问：

- `GET /api/v1/document-workspaces`
- `GET /api/v1/documents`
- `GET /api/v1/documents/{id}`
- `POST /api/v1/documents`
- `PUT /api/v1/documents/{id}`
- `DELETE /api/v1/documents/{id}`
