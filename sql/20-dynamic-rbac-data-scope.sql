USE oa_system;

-- 数据范围本身也是 RBAC 权限。自定义角色无需依赖固定角色编码。
INSERT INTO sys_permission (id, parent_id, code, name, type, path, deleted)
VALUES
    (38, 0, 'data:scope:all', '全部数据范围', 'DATA', NULL, 0),
    (39, 0, 'data:scope:department', '本部门数据范围', 'DATA', NULL, 0),
    (40, 0, 'data:scope:self', '仅本人数据范围', 'DATA', NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), path = VALUES(path), deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code = CASE r.code
    WHEN 'ADMIN' THEN 'data:scope:all'
    WHEN 'HR' THEN 'data:scope:all'
    WHEN 'MANAGER' THEN 'data:scope:department'
    ELSE 'data:scope:self'
END
WHERE r.deleted = 0 AND p.deleted = 0;
