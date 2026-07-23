USE oa_system;

-- 已执行过 01-initial-schema.sql 的数据库只需执行本升级脚本。
-- 权限 ID 13-34 已由 RBAC、员工管理和考勤模块使用，审批权限使用 35。
CREATE TABLE IF NOT EXISTS flow_action_log (
    id BIGINT PRIMARY KEY COMMENT '审批操作日志主键',
    request_id BIGINT NOT NULL COMMENT '流程申请ID',
    operator_id BIGINT NOT NULL COMMENT '审批人ID',
    action VARCHAR(32) NOT NULL COMMENT '审批动作：APPROVE/REJECT',
    comment VARCHAR(500) NULL COMMENT '审批意见',
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_flow_action_request (request_id, operated_at),
    KEY idx_flow_action_operator (operator_id, operated_at)
) COMMENT='审批操作日志';

-- 先创建新权限，确保迁移角色关系时外键目标已经存在。
INSERT INTO sys_permission (id, parent_id, code, name, type, path, deleted)
VALUES (35, 0, 'flow:task:approve', '处理审批任务', 'BUTTON', NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), deleted = 0;

-- 兼容此前可能使用冲突 ID 15 或 30 创建过的审批权限。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT rp.role_id, 35
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'flow:task:approve' AND p.id <> 35;

DELETE rp
FROM sys_role_permission rp
JOIN sys_permission p ON p.id = rp.permission_id
WHERE p.code = 'flow:task:approve' AND p.id <> 35;

DELETE FROM sys_permission
WHERE code = 'flow:task:approve' AND id <> 35;

INSERT INTO sys_permission (id, parent_id, code, name, type, path, deleted)
VALUES (35, 0, 'flow:task:approve', '处理审批任务', 'BUTTON', NULL, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), type = VALUES(type), deleted = 0;

-- 管理员和部门主管获得审批权限。
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT id, 35
FROM sys_role
WHERE code IN ('ADMIN', 'MANAGER') AND status = 1 AND deleted = 0;
