USE oa_system;

-- 修复曾因客户端字符集错误写入的审批权限名称。
UPDATE sys_permission
SET name = '处理审批任务', type = 'BUTTON', path = NULL, deleted = 0
WHERE code = 'flow:task:approve';

-- 职责分离：HR 可查看部门结构，但只有管理员可创建、修改和删除部门。
DELETE rp
FROM sys_role_permission rp
JOIN sys_role r ON r.id = rp.role_id
JOIN sys_permission p ON p.id = rp.permission_id
WHERE r.code = 'HR'
  AND p.code IN ('sys:dept:create', 'sys:dept:update', 'sys:dept:delete');
