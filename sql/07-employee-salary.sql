USE oa_system;

-- 为已有数据库增加员工月基本薪资字段，可重复执行。
SET @salary_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'salary'
);
SET @salary_column_sql = IF(
    @salary_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN salary DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''月基本薪资'' AFTER email',
    'SELECT 1'
);
PREPARE salary_column_statement FROM @salary_column_sql;
EXECUTE salary_column_statement;
DEALLOCATE PREPARE salary_column_statement;

INSERT INTO sys_permission (id, parent_id, code, name, type, path, deleted)
VALUES
    (36, 0, 'sys:salary:view', '查看员工薪资', 'API', '/api/v1/users', 0),
    (37, 0, 'sys:salary:update', '调整员工薪资', 'API', '/api/v1/users/{id}/salary', 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), path = VALUES(path), deleted = 0;

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM sys_role r
JOIN sys_permission p ON p.code IN ('sys:user:list', 'sys:salary:view', 'sys:salary:update') AND p.deleted = 0
WHERE r.code IN ('ADMIN', 'HR', 'MANAGER') AND r.status = 1 AND r.deleted = 0;
