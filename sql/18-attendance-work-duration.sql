USE oa_system;

SET @actual_work_minutes_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'attendance_record'
      AND COLUMN_NAME = 'actual_work_minutes'
);
SET @actual_work_minutes_sql = IF(
    @actual_work_minutes_exists = 0,
    'ALTER TABLE attendance_record ADD COLUMN actual_work_minutes INT NOT NULL DEFAULT 0 COMMENT ''实际工时分钟数'' AFTER early_leave_minutes',
    'SELECT 1'
);
PREPARE actual_work_minutes_statement FROM @actual_work_minutes_sql;
EXECUTE actual_work_minutes_statement;
DEALLOCATE PREPARE actual_work_minutes_statement;

UPDATE attendance_record
SET actual_work_minutes = GREATEST(0, TIMESTAMPDIFF(MINUTE, check_in_time, check_out_time))
WHERE check_in_time IS NOT NULL
  AND check_out_time IS NOT NULL;
