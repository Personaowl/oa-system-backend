USE oa_system;

-- 演示环境初始化：只保留四个可登录测试账号，密码均为 123456。
-- 其他账号采用逻辑删除，避免破坏历史公告、考勤和审批的审计记录。
START TRANSACTION;

UPDATE sys_department
SET manager_id = NULL
WHERE manager_id IS NOT NULL
  AND manager_id NOT IN (10001, 10002, 10003, 10004);

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
WHERE u.username NOT IN ('mty-admin', 'mty-hr', 'mty-manager', 'mty-employee');

UPDATE sys_user
SET username = CONCAT('archived_', id),
    department_id = NULL,
    status = 0,
    deleted = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE username NOT IN ('mty-admin', 'mty-hr', 'mty-manager', 'mty-employee')
  AND deleted = 0;

UPDATE sys_department
SET name = CONCAT('archived_', id), manager_id = NULL, status = 0, deleted = 1
WHERE id NOT IN (1, 2001, 2002, 2003, 2004) AND deleted = 0;

INSERT INTO sys_department (id, parent_id, name, manager_id, sort_order, status, deleted)
VALUES
    (1, 0, '总部', NULL, 0, 1, 0),
    (2001, 1, '人力资源部', NULL, 10, 1, 0),
    (2002, 1, '研发部', 10003, 20, 1, 0),
    (2003, 1, '财务部', NULL, 30, 1, 0),
    (2004, 1, '市场部', NULL, 40, 1, 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), name = VALUES(name), manager_id = VALUES(manager_id),
    sort_order = VALUES(sort_order), status = 1, deleted = 0;

INSERT INTO sys_user (
    id, department_id, username, password_hash, display_name, phone, email, salary, status, deleted
)
VALUES
    (10001, 1, 'mty-admin', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 超级管理员', '13800000001', 'mty-admin@oa.local', 28000.00, 1, 0),
    (10002, 2001, 'mty-hr', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY HR', '13800000002', 'mty-hr@oa.local', 18000.00, 1, 0),
    (10003, 2002, 'mty-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 研发主管', '13800000003', 'mty-manager@oa.local', 22000.00, 1, 0),
    (10004, 2002, 'mty-employee', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', 'MTY 研发员工', '13800000004', 'mty-employee@oa.local', 12000.00, 1, 0)
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id), password_hash = VALUES(password_hash), display_name = VALUES(display_name),
    phone = VALUES(phone), email = VALUES(email), salary = VALUES(salary), status = 1, deleted = 0;

DELETE ur
FROM sys_user_role ur
JOIN sys_user u ON u.id = ur.user_id
WHERE u.username IN ('mty-admin', 'mty-hr', 'mty-manager', 'mty-employee');

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM (
    SELECT 'mty-admin' username, 'ADMIN' role_code
    UNION ALL SELECT 'mty-hr', 'HR'
    UNION ALL SELECT 'mty-manager', 'MANAGER'
    UNION ALL SELECT 'mty-employee', 'EMPLOYEE'
) mapping
JOIN sys_user u ON u.username = mapping.username AND u.deleted = 0
JOIN sys_role r ON r.code = mapping.role_code AND r.status = 1 AND r.deleted = 0;

UPDATE sys_department SET manager_id = 10003 WHERE id = 2002;

-- 公告演示数据。
INSERT INTO notice (id, title, summary, content, publisher_id, status, top_flag, published_at, view_count, created_by, updated_by, deleted, version)
VALUES
    (81001, '欢迎使用 OA 办公管理系统', '系统功能与演示账号说明', '欢迎使用 OA 办公管理系统。请通过左侧菜单体验组织、考勤、审批和公告功能。', 10001, 'PUBLISHED', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), 26, 10001, 10001, 0, 0),
    (81002, '研发部本周例会通知', '周五下午进行研发周会', '研发部本周五 15:00 在第一会议室召开周会，请相关同事准时参加。', 10003, 'PUBLISHED', 0, DATE_SUB(NOW(), INTERVAL 1 DAY), 12, 10003, 10003, 0, 0),
    (81003, '员工考勤与审批使用指引', '打卡、请假和加班申请说明', '请每日按时完成上下班打卡；请假和加班申请通过审批流程页面提交。', 10002, 'PUBLISHED', 0, NOW(), 5, 10002, 10002, 0, 0)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), summary = VALUES(summary), content = VALUES(content), publisher_id = VALUES(publisher_id),
    status = VALUES(status), top_flag = VALUES(top_flag), published_at = VALUES(published_at), deleted = 0;

-- 最近几天的考勤演示数据。
INSERT INTO attendance_record (
    id, user_id, work_date, check_in_time, check_out_time, status, late_minutes,
    early_leave_minutes, rule_work_start, rule_work_end, rule_late_threshold_minutes, version
)
VALUES
    (82001, 10003, DATE_SUB(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '08:55:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '18:06:00'), 'NORMAL', 0, 0, '09:00:00', '18:00:00', 5, 0),
    (82002, 10004, DATE_SUB(CURDATE(), INTERVAL 2 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:12:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '18:03:00'), 'LATE', 7, 0, '09:00:00', '18:00:00', 5, 0),
    (82003, 10003, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:58:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '18:10:00'), 'NORMAL', 0, 0, '09:00:00', '18:00:00', 5, 0),
    (82004, 10004, DATE_SUB(CURDATE(), INTERVAL 1 DAY), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '08:57:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '17:42:00'), 'EARLY_LEAVE', 0, 18, '09:00:00', '18:00:00', 5, 0)
ON DUPLICATE KEY UPDATE
    check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), status = VALUES(status),
    late_minutes = VALUES(late_minutes), early_leave_minutes = VALUES(early_leave_minutes),
    rule_work_start = VALUES(rule_work_start), rule_work_end = VALUES(rule_work_end),
    rule_late_threshold_minutes = VALUES(rule_late_threshold_minutes);

-- 审批演示数据：一条待主管审批、一条已完成。
INSERT INTO flow_request (id, applicant_id, request_type, start_time, end_time, reason, status, current_approver_id, created_at, updated_at)
VALUES
    (83001, 10004, 'LEAVE', DATE_ADD(CURDATE(), INTERVAL 2 DAY), DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 2 DAY), INTERVAL 8 HOUR), '参加个人事务，需要请假一天。', 'PENDING', 10003, NOW(), NOW()),
    (83002, 10004, 'OVERTIME', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 2 HOUR, '完成版本发布与线上验证。', 'APPROVED', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
    applicant_id = VALUES(applicant_id), request_type = VALUES(request_type), start_time = VALUES(start_time),
    end_time = VALUES(end_time), reason = VALUES(reason), status = VALUES(status),
    current_approver_id = VALUES(current_approver_id), updated_at = VALUES(updated_at);

INSERT INTO flow_action_log (id, request_id, operator_id, action, comment, operated_at)
VALUES (83101, 83002, 10003, 'APPROVE', '同意加班申请，请注意调休安排。', DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE comment = VALUES(comment), operated_at = VALUES(operated_at);

COMMIT;
