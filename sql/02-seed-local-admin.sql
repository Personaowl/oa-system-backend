USE oa_system;

-- Local development account: admin / 123456
-- The password stored below is a BCrypt hash, not plaintext.
INSERT INTO sys_department (id, parent_id, name, sort_order, status, deleted)
VALUES (1, 0, '总部', 0, 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

INSERT INTO sys_user (
    id, department_id, username, password_hash, display_name, phone, email, status, deleted
)
VALUES (
    1, 1, 'admin', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC',
    '系统管理员', NULL, 'admin@oa.local', 1, 0
)
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    password_hash = VALUES(password_hash),
    display_name = VALUES(display_name),
    status = 1,
    deleted = 0;

INSERT INTO sys_role (id, code, name, status, deleted)
VALUES
    (1, 'ADMIN', '系统管理员', 1, 0),
    (2, 'EMPLOYEE', '普通员工', 1, 0),
    (3, 'HR', 'HR 人事', 1, 0),
    (4, 'MANAGER', '部门主管', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

-- Migrate attendance permissions created by earlier seeds at conflicting IDs.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT rp.role_id, 28
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'attendance:record:query';

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT rp.role_id, 29
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'attendance:statistics:query';

DELETE rp
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code IN ('attendance:record:query', 'attendance:statistics:query')
  AND p.id NOT IN (28, 29);

DELETE FROM sys_permission
WHERE code IN ('attendance:record:query', 'attendance:statistics:query')
  AND id NOT IN (28, 29);

-- Migrate the approval permission created by earlier flow seeds at a conflicting ID.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT rp.role_id, 30
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'flow:task:approve';

DELETE rp
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'flow:task:approve' AND p.id <> 30;

DELETE FROM sys_permission
WHERE code = 'flow:task:approve' AND id <> 30;

INSERT INTO sys_permission (id, parent_id, code, name, type, path, deleted)
VALUES
    (1, 0, 'user:read', '查看用户', 'BUTTON', NULL, 0),
    (2, 0, 'attendance:read', '查看考勤', 'BUTTON', NULL, 0),
    (3, 0, 'flow:read', '查看审批', 'BUTTON', NULL, 0),
    (4, 0, 'notice:read', '查看公告', 'BUTTON', NULL, 0),
    (5, 0, 'system:admin', '系统管理', 'BUTTON', NULL, 0),
    (6, 0, 'notice:create', '创建公告', 'BUTTON', NULL, 0),
    (7, 0, 'notice:update', '修改公告', 'BUTTON', NULL, 0),
    (8, 0, 'notice:delete', '删除公告', 'BUTTON', NULL, 0),
    (9, 0, 'notice:publish', '发布公告', 'BUTTON', NULL, 0),
    (10, 0, 'notice:offline', '下线公告', 'BUTTON', NULL, 0),
    (11, 0, 'notice:list', '公告列表', 'BUTTON', NULL, 0),
    (12, 0, 'notice:view', '公告详情', 'BUTTON', NULL, 0),
    (13, 0, 'sys:dept:list', '部门列表', 'API', '/api/v1/departments', 0),
    (14, 0, 'sys:dept:view', '部门详情', 'API', '/api/v1/departments/{id}', 0),
    (15, 0, 'sys:dept:create', '创建部门', 'API', '/api/v1/departments', 0),
    (16, 0, 'sys:dept:update', '修改部门', 'API', '/api/v1/departments/{id}', 0),
    (17, 0, 'sys:dept:delete', '删除部门', 'API', '/api/v1/departments/{id}', 0),
    (18, 0, 'sys:role:list', '角色列表', 'API', '/api/v1/roles', 0),
    (19, 0, 'sys:role:view', '角色详情', 'API', '/api/v1/roles/{id}', 0),
    (20, 0, 'sys:role:create', '创建角色', 'API', '/api/v1/roles', 0),
    (21, 0, 'sys:role:update', '修改角色', 'API', '/api/v1/roles/{id}', 0),
    (22, 0, 'sys:role:delete', '删除角色', 'API', '/api/v1/roles/{id}', 0),
    (23, 0, 'sys:role:assign-permission', '分配角色权限', 'API', '/api/v1/roles/{id}/permissions', 0),
    (24, 0, 'sys:permission:list', '权限列表', 'API', '/api/v1/permissions', 0),
    (25, 0, 'sys:user:role:list', '用户角色列表', 'API', '/api/v1/users/{id}/roles', 0),
    (26, 0, 'sys:user:assign-role', '分配用户角色', 'API', '/api/v1/users/{id}/roles', 0),
    (27, 0, 'ai:chat', 'AI办公助手', 'API', '/api/v1/ai/chat', 0),
    (28, 0, 'attendance:record:query', '查询全员考勤记录', 'BUTTON', NULL, 0),
    (29, 0, 'attendance:statistics:query', '查询考勤汇总统计', 'BUTTON', NULL, 0),
    (30, 0, 'flow:task:approve', '处理审批任务', 'BUTTON', NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id
FROM sys_permission
WHERE deleted = 0;

-- Registered users receive the least-privilege employee role by default.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 2, id
FROM sys_permission
WHERE code IN ('attendance:read', 'flow:read', 'notice:read', 'ai:chat');

-- HR can maintain organization data and the full notice lifecycle.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 3, id
FROM sys_permission
WHERE code IN (
    'user:read', 'attendance:read', 'flow:read', 'notice:read',
    'notice:create', 'notice:update', 'notice:delete', 'notice:publish',
    'notice:offline', 'notice:list', 'notice:view',
    'sys:dept:list', 'sys:dept:view', 'sys:dept:create', 'sys:dept:update', 'sys:dept:delete',
    'sys:role:list', 'sys:role:view', 'sys:permission:list',
    'sys:user:role:list', 'sys:user:assign-role', 'ai:chat',
    'attendance:record:query', 'attendance:statistics:query'
);

-- Department managers receive business read/review permissions only.
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 4, id
FROM sys_permission
WHERE code IN (
    'user:read', 'attendance:read', 'flow:read', 'notice:read', 'ai:chat',
    'flow:task:approve'
);

-- Backfill accounts created before default role assignment was introduced.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT u.id, 2
FROM sys_user u
LEFT JOIN sys_user_role ur ON ur.user_id = u.id
WHERE u.deleted = 0 AND ur.user_id IS NULL;
