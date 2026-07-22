USE oa_system;

-- 扩充演示组织数据。所有 demo-* 账号密码均为 123456，可重复执行。
START TRANSACTION;

INSERT INTO sys_department (id, parent_id, name, manager_id, sort_order, status, deleted)
VALUES
    (2005, 1, '产品部', 11007, 50, 1, 0),
    (2006, 1, '运营部', 11010, 60, 1, 0),
    (2007, 1, '行政部', 11013, 70, 1, 0),
    (2008, 1, '客户成功部', 11016, 80, 1, 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), name = VALUES(name), manager_id = VALUES(manager_id),
    sort_order = VALUES(sort_order), status = 1, deleted = 0;

INSERT INTO sys_user (
    id, department_id, username, password_hash, display_name, phone, email, salary, status, deleted
)
VALUES
    (11001, 2003, 'demo-finance-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '周敏', '13900010001', 'zhou.min@oa.demo', 19000.00, 1, 0),
    (11002, 2003, 'demo-finance-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '许静', '13900010002', 'xu.jing@oa.demo', 11000.00, 1, 0),
    (11003, 2003, 'demo-finance-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '韩雪', '13900010003', 'han.xue@oa.demo', 10500.00, 1, 0),
    (11004, 2004, 'demo-marketing-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '陈晨', '13900010004', 'chen.chen@oa.demo', 18500.00, 1, 0),
    (11005, 2004, 'demo-marketing-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '林悦', '13900010005', 'lin.yue@oa.demo', 11500.00, 1, 0),
    (11006, 2004, 'demo-marketing-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '高宇', '13900010006', 'gao.yu@oa.demo', 10800.00, 1, 0),
    (11007, 2005, 'demo-product-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '赵磊', '13900010007', 'zhao.lei@oa.demo', 21000.00, 1, 0),
    (11008, 2005, 'demo-product-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '苏晴', '13900010008', 'su.qing@oa.demo', 14500.00, 1, 0),
    (11009, 2005, 'demo-product-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '唐杰', '13900010009', 'tang.jie@oa.demo', 13800.00, 1, 0),
    (11010, 2006, 'demo-operations-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '吴芳', '13900010010', 'wu.fang@oa.demo', 17800.00, 1, 0),
    (11011, 2006, 'demo-operations-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '何嘉', '13900010011', 'he.jia@oa.demo', 10200.00, 1, 0),
    (11012, 2006, 'demo-operations-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '蒋欣', '13900010012', 'jiang.xin@oa.demo', 9800.00, 1, 0),
    (11013, 2007, 'demo-admin-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '郑雅', '13900010013', 'zheng.ya@oa.demo', 16500.00, 1, 0),
    (11014, 2007, 'demo-admin-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '彭佳', '13900010014', 'peng.jia@oa.demo', 9000.00, 1, 0),
    (11015, 2007, 'demo-admin-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '罗倩', '13900010015', 'luo.qian@oa.demo', 9200.00, 1, 0),
    (11016, 2008, 'demo-customer-manager', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '孙浩', '13900010016', 'sun.hao@oa.demo', 18000.00, 1, 0),
    (11017, 2008, 'demo-customer-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '杜琳', '13900010017', 'du.lin@oa.demo', 11200.00, 1, 0),
    (11018, 2008, 'demo-customer-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '魏然', '13900010018', 'wei.ran@oa.demo', 11800.00, 1, 0),
    (11019, 2002, 'demo-rd-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '李明', '13900010019', 'li.ming@oa.demo', 15500.00, 1, 0),
    (11020, 2002, 'demo-rd-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '王凯', '13900010020', 'wang.kai@oa.demo', 16000.00, 1, 0),
    (11021, 2002, 'demo-rd-03', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '张雨', '13900010021', 'zhang.yu@oa.demo', 14800.00, 1, 0),
    (11022, 2002, 'demo-rd-04', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '郭鹏', '13900010022', 'guo.peng@oa.demo', 15200.00, 1, 0),
    (11023, 2002, 'demo-rd-05', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '刘婷', '13900010023', 'liu.ting@oa.demo', 14600.00, 1, 0),
    (11024, 2001, 'demo-hr-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '杨帆', '13900010024', 'yang.fan@oa.demo', 10800.00, 1, 0),
    (11025, 2001, 'demo-hr-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '冯璐', '13900010025', 'feng.lu@oa.demo', 10500.00, 1, 0),
    (11026, 2001, 'demo-hr-03', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '马宁', '13900010026', 'ma.ning@oa.demo', 11200.00, 1, 0),
    (11027, 1, 'demo-hq-01', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '谢文', '13900010027', 'xie.wen@oa.demo', 12500.00, 1, 0),
    (11028, 1, 'demo-hq-02', '$2a$10$NZ7Z9BNkGzBrD/xpLCl0Ru/VCPr5RY7JJWHfQij2re1E./99CssHC', '宋妍', '13900010028', 'song.yan@oa.demo', 12800.00, 1, 0)
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id), password_hash = VALUES(password_hash), display_name = VALUES(display_name),
    phone = VALUES(phone), email = VALUES(email), salary = VALUES(salary), status = 1, deleted = 0;

DELETE FROM sys_user_role WHERE user_id BETWEEN 11001 AND 11028;

INSERT INTO sys_user_role (user_id, role_id)
SELECT u.id, r.id
FROM sys_user u
JOIN sys_role r ON r.code = CASE
    WHEN u.id IN (11001, 11004, 11007, 11010, 11013, 11016) THEN 'MANAGER'
    ELSE 'EMPLOYEE'
END
WHERE u.id BETWEEN 11001 AND 11028 AND u.deleted = 0 AND r.status = 1 AND r.deleted = 0;

UPDATE sys_department SET manager_id = 10002 WHERE id = 2001;
UPDATE sys_department SET manager_id = 10003 WHERE id = 2002;
UPDATE sys_department SET manager_id = 11001 WHERE id = 2003;
UPDATE sys_department SET manager_id = 11004 WHERE id = 2004;
UPDATE sys_department SET manager_id = 11007 WHERE id = 2005;
UPDATE sys_department SET manager_id = 11010 WHERE id = 2006;
UPDATE sys_department SET manager_id = 11013 WHERE id = 2007;
UPDATE sys_department SET manager_id = 11016 WHERE id = 2008;

-- 为新增模拟员工生成最近三个工作日的考勤，包含正常、迟到和早退状态。
INSERT INTO attendance_record (
    id, user_id, work_date, check_in_time, check_out_time, status, late_minutes,
    early_leave_minutes, rule_work_start, rule_work_end, rule_late_threshold_minutes, version
)
SELECT
    900000000 + u.id * 10 + days.day_no,
    u.id,
    DATE_SUB(CURDATE(), INTERVAL days.day_no DAY),
    TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL days.day_no DAY),
        CASE WHEN MOD(u.id + days.day_no, 5) = 0 THEN '09:13:00' ELSE '08:56:00' END),
    TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL days.day_no DAY),
        CASE WHEN MOD(u.id + days.day_no, 7) = 0 THEN '17:42:00' ELSE '18:08:00' END),
    CASE
        WHEN MOD(u.id + days.day_no, 5) = 0 AND MOD(u.id + days.day_no, 7) = 0 THEN 'LATE_AND_EARLY_LEAVE'
        WHEN MOD(u.id + days.day_no, 5) = 0 THEN 'LATE'
        WHEN MOD(u.id + days.day_no, 7) = 0 THEN 'EARLY_LEAVE'
        ELSE 'NORMAL'
    END,
    CASE WHEN MOD(u.id + days.day_no, 5) = 0 THEN 8 ELSE 0 END,
    CASE WHEN MOD(u.id + days.day_no, 7) = 0 THEN 18 ELSE 0 END,
    '09:00:00', '18:00:00', 5, 0
FROM sys_user u
JOIN (SELECT 1 day_no UNION ALL SELECT 2 UNION ALL SELECT 3) days
WHERE u.id BETWEEN 11001 AND 11028 AND u.deleted = 0
ON DUPLICATE KEY UPDATE
    check_in_time = VALUES(check_in_time), check_out_time = VALUES(check_out_time), status = VALUES(status),
    late_minutes = VALUES(late_minutes), early_leave_minutes = VALUES(early_leave_minutes),
    rule_work_start = VALUES(rule_work_start), rule_work_end = VALUES(rule_work_end),
    rule_late_threshold_minutes = VALUES(rule_late_threshold_minutes);

COMMIT;
