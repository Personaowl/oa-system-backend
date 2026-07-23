USE oa_system;

-- 可重复执行：部门负责人是组织关系，不强制要求员工拥有 MANAGER 权限角色。
SET @manager_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_department'
      AND COLUMN_NAME = 'manager_id'
);
SET @add_manager_column_sql = IF(
    @manager_column_exists = 0,
    'ALTER TABLE sys_department ADD COLUMN manager_id BIGINT NULL COMMENT ''部门负责人用户ID'' AFTER name',
    'SELECT 1'
);
PREPARE add_manager_column_stmt FROM @add_manager_column_sql;
EXECUTE add_manager_column_stmt;
DEALLOCATE PREPARE add_manager_column_stmt;

SET @manager_index_exists = (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_department'
      AND INDEX_NAME = 'idx_department_manager'
);
SET @add_manager_index_sql = IF(
    @manager_index_exists = 0,
    'ALTER TABLE sys_department ADD KEY idx_department_manager (manager_id)',
    'SELECT 1'
);
PREPARE add_manager_index_stmt FROM @add_manager_index_sql;
EXECUTE add_manager_index_stmt;
DEALLOCATE PREPARE add_manager_index_stmt;
