USE oa_system;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS attendance_calendar_day (
    work_date DATE PRIMARY KEY COMMENT '日期',
    day_type VARCHAR(16) NOT NULL COMMENT '日期类型：WORKDAY/HOLIDAY',
    holiday_name VARCHAR(100) NULL COMMENT '节假日或调休说明',
    updated_by BIGINT NULL COMMENT '最后修改人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_calendar_day_type (day_type, work_date)
) COMMENT='企业工作日历覆盖';

CREATE TABLE IF NOT EXISTS attendance_shift (
    id BIGINT PRIMARY KEY COMMENT '班次主键',
    name VARCHAR(64) NOT NULL COMMENT '班次名称',
    work_start TIME NOT NULL COMMENT '上班时间',
    work_end TIME NOT NULL COMMENT '下班时间',
    late_threshold_minutes INT NOT NULL DEFAULT 5 COMMENT '迟到宽限分钟数',
    color VARCHAR(16) NOT NULL DEFAULT '#409eff' COMMENT '前端展示颜色',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认班次',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_attendance_shift_name (name),
    KEY idx_shift_default_status (is_default, status)
) COMMENT='考勤班次';

CREATE TABLE IF NOT EXISTS attendance_shift_assignment (
    id BIGINT PRIMARY KEY COMMENT '人员排班主键',
    user_id BIGINT NOT NULL COMMENT '员工ID',
    shift_id BIGINT NOT NULL COMMENT '班次ID',
    start_date DATE NOT NULL COMMENT '生效开始日期',
    end_date DATE NOT NULL COMMENT '生效结束日期',
    created_by BIGINT NULL COMMENT '排班操作人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_assignment_user_date (user_id, start_date, end_date),
    KEY idx_assignment_shift (shift_id)
) COMMENT='员工班次分配';

-- 将当前全局考勤规则作为默认班次，保留已有配置。
INSERT INTO attendance_shift (
    id, name, work_start, work_end, late_threshold_minutes,
    color, is_default, status
)
SELECT 1400001, '标准班次', work_start, work_end, late_threshold_minutes,
       '#409eff', 1, 1
FROM attendance_rule
WHERE id = 1
ON DUPLICATE KEY UPDATE
    work_start = VALUES(work_start),
    work_end = VALUES(work_end),
    late_threshold_minutes = VALUES(late_threshold_minutes),
    is_default = 1,
    status = 1;
