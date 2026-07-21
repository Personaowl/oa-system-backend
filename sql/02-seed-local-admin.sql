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
VALUES (1, 'ADMIN', '系统管理员', 1, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 1, deleted = 0;

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
    (12, 0, 'notice:view', '公告详情', 'BUTTON', NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id) VALUES (1, 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
VALUES (1, 1), (1, 2), (1, 3), (1, 4), (1, 5), (1, 6), (1, 7),
       (1, 8), (1, 9), (1, 10), (1, 11), (1, 12);
