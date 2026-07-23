USE oa_system;
SET NAMES utf8mb4;

-- 审批增强：自动审批人、分类字段、撤回、完整时间线和考勤联动。
-- 本脚本仅需执行一次；全新数据库已同步更新 01-initial-schema.sql。

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_request' AND COLUMN_NAME = 'leave_type'),
    'SELECT 1',
    'ALTER TABLE flow_request ADD COLUMN leave_type VARCHAR(32) NULL COMMENT ''请假类型：PERSONAL/SICK/ANNUAL/COMPENSATORY'' AFTER reason'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_request' AND COLUMN_NAME = 'overtime_compensation'),
    'SELECT 1',
    'ALTER TABLE flow_request ADD COLUMN overtime_compensation VARCHAR(32) NULL COMMENT ''加班补偿：PAY/COMPENSATORY'' AFTER leave_type'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_request' AND COLUMN_NAME = 'duration_minutes'),
    'SELECT 1',
    'ALTER TABLE flow_request ADD COLUMN duration_minutes INT NOT NULL DEFAULT 0 COMMENT ''申请时长（分钟）'' AFTER overtime_compensation'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

SET @ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'flow_request' AND INDEX_NAME = 'idx_flow_applicant_time'),
    'SELECT 1',
    'ALTER TABLE flow_request ADD KEY idx_flow_applicant_time (applicant_id, start_time, end_time)'
);
PREPARE statement FROM @ddl;
EXECUTE statement;
DEALLOCATE PREPARE statement;

ALTER TABLE flow_request
    MODIFY COLUMN leave_type VARCHAR(32) NULL
        COMMENT '请假类型：PERSONAL/SICK/ANNUAL/COMPENSATORY',
    MODIFY COLUMN overtime_compensation VARCHAR(32) NULL
        COMMENT '加班补偿：PAY/COMPENSATORY',
    MODIFY COLUMN duration_minutes INT NOT NULL DEFAULT 0
        COMMENT '申请时长（分钟）';

UPDATE flow_request
SET duration_minutes = TIMESTAMPDIFF(MINUTE, start_time, end_time)
WHERE duration_minutes = 0;

UPDATE flow_request
SET leave_type = 'PERSONAL'
WHERE request_type = 'LEAVE' AND leave_type IS NULL;

UPDATE flow_request
SET overtime_compensation = 'COMPENSATORY'
WHERE request_type = 'OVERTIME' AND overtime_compensation IS NULL;

-- 旧申请补充“提交”时间线，避免详情页只有审批结果。
INSERT INTO flow_action_log (id, request_id, operator_id, action, comment, operated_at)
SELECT
    CAST(CONCAT('88', LPAD(ROW_NUMBER() OVER (ORDER BY r.id), 16, '0')) AS UNSIGNED),
    r.id,
    r.applicant_id,
    'SUBMIT',
    '提交申请',
    r.created_at
FROM flow_request r
WHERE NOT EXISTS (
    SELECT 1 FROM flow_action_log l
    WHERE l.request_id = r.id AND l.action = 'SUBMIT'
);
