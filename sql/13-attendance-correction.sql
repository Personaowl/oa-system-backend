USE oa_system;

CREATE TABLE IF NOT EXISTS attendance_correction (
    id BIGINT PRIMARY KEY COMMENT '补卡申请主键',
    user_id BIGINT NOT NULL COMMENT '申请人ID',
    work_date DATE NOT NULL COMMENT '补卡归属工作日',
    correction_type VARCHAR(32) NOT NULL COMMENT '补卡类型：CHECK_IN/CHECK_OUT',
    correction_time DATETIME NOT NULL COMMENT '申请补录的打卡时间',
    reason VARCHAR(500) NOT NULL COMMENT '补卡原因',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
    approver_id BIGINT NULL COMMENT '实际审批人ID',
    review_comment VARCHAR(500) NULL COMMENT '审批意见',
    decided_at DATETIME NULL COMMENT '审批时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_correction_user_date (user_id, work_date),
    KEY idx_correction_status_created (status, created_at)
) COMMENT='考勤补卡申请';
