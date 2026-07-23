USE oa_system;

CREATE TABLE IF NOT EXISTS attendance_rule (
    id BIGINT PRIMARY KEY COMMENT '规则主键，当前固定为1',
    work_start TIME NOT NULL COMMENT '上班时间',
    work_end TIME NOT NULL COMMENT '下班时间',
    late_threshold_minutes INT NOT NULL DEFAULT 5 COMMENT '迟到宽限分钟数',
    updated_by BIGINT NULL COMMENT '最后修改人ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='当前考勤规则';

INSERT INTO attendance_rule (id, work_start, work_end, late_threshold_minutes)
VALUES (1, '09:00:00', '18:00:00', 5)
ON DUPLICATE KEY UPDATE id = VALUES(id);
