USE oa_system;
SET NAMES utf8mb4;

-- 部门支持一名主负责人和多名协同主管；可重复执行。
CREATE TABLE IF NOT EXISTS sys_department_manager (
    department_id BIGINT NOT NULL COMMENT '部门ID',
    user_id BIGINT NOT NULL COMMENT '主管用户ID',
    is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主负责人：1是，0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '配置时间',
    PRIMARY KEY (department_id, user_id),
    KEY idx_department_manager_user (user_id, department_id)
) COMMENT='部门主管关联';

-- 将旧 manager_id 平滑迁移为主负责人。
INSERT INTO sys_department_manager (department_id, user_id, is_primary, created_at)
SELECT id, manager_id, 1, CURRENT_TIMESTAMP
FROM sys_department
WHERE manager_id IS NOT NULL AND deleted = 0
ON DUPLICATE KEY UPDATE is_primary = VALUES(is_primary);
