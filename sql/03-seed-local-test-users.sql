USE oa_system;

-- Local test accounts. All passwords are: 123456
-- The stored value is the same BCrypt hash used by the local admin account.
INSERT INTO sys_user (
    id, department_id, username, password_hash, display_name, phone, email, status, deleted
)
VALUES
    (10001, 1, 'mty-admin', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 超级管理员', NULL, 'mty-admin@oa.local', 1, 0),
    (10002, 1, 'mty-hr', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY HR', NULL, 'mty-hr@oa.local', 1, 0),
    (10003, 1, 'mty-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 部门主管', NULL, 'mty-manager@oa.local', 1, 0),
    (10004, 1, 'mty-employee', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 普通员工', NULL, 'mty-employee@oa.local', 1, 0)
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    password_hash = VALUES(password_hash),
    display_name = VALUES(display_name),
    email = VALUES(email),
    status = 1,
    deleted = 0;

-- Replace role assignments so each account always represents exactly one role.
DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
WHERE u.username IN ('mty-admin', 'mty-hr', 'mty-manager', 'mty-employee');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM (
    SELECT 'mty-admin' AS username, 'ADMIN' AS role_code
    UNION ALL SELECT 'mty-hr', 'HR'
    UNION ALL SELECT 'mty-manager', 'MANAGER'
    UNION ALL SELECT 'mty-employee', 'EMPLOYEE'
) account_role
JOIN sys_user u ON u.username = account_role.username
JOIN sys_role r ON r.code = account_role.role_code AND r.status = 1 AND r.deleted = 0;

-- Give the local manager account a real department data scope without overwriting
-- a manager explicitly configured later through the organization page.
UPDATE sys_department d
JOIN sys_user u ON u.department_id = d.id AND u.username = 'mty-manager'
SET d.manager_id = u.id
WHERE d.deleted = 0 AND d.manager_id IS NULL;
