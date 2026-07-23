USE oa_system;

-- Run once after 01-initial-schema.sql. Script number 02 is already used by local seed data.
ALTER TABLE attendance_record
    MODIFY check_in_time DATETIME(3) NULL,
    MODIFY check_out_time DATETIME(3) NULL,
    MODIFY created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    MODIFY updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    ADD COLUMN late_minutes INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN early_leave_minutes INT NOT NULL DEFAULT 0 AFTER late_minutes,
    ADD COLUMN rule_work_start TIME NULL AFTER early_leave_minutes,
    ADD COLUMN rule_work_end TIME NULL AFTER rule_work_start,
    ADD COLUMN rule_late_threshold_minutes INT NULL AFTER rule_work_end,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER updated_at,
    ADD KEY idx_attendance_work_date_status (work_date, status);

-- Existing final idempotency constraint remains unchanged:
-- UNIQUE KEY uk_attendance_user_date (user_id, work_date)
