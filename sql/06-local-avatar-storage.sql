USE oa_system;

-- 已执行过 01-initial-schema.sql 的数据库执行本升级脚本。
SET @avatar_column_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_user'
      AND COLUMN_NAME = 'avatar_file_name'
);
SET @avatar_column_sql = IF(
    @avatar_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN avatar_file_name VARCHAR(255) NULL COMMENT ''本地头像文件名'' AFTER display_name',
    'SELECT 1'
);
PREPARE avatar_column_statement FROM @avatar_column_sql;
EXECUTE avatar_column_statement;
DEALLOCATE PREPARE avatar_column_statement;
