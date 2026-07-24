USE oa_system;

-- 独立薪资管理：职级基础工资 + 绩效工资 - 扣除工资。
-- 可重复执行，适用于已经存在 sys_user.salary 的数据库。
SET @grade_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'salary_grade'
);
SET @grade_column_sql = IF(
    @grade_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN salary_grade VARCHAR(3) NOT NULL DEFAULT ''13A'' COMMENT ''薪资职级：13A至20C'' AFTER salary',
    'SELECT 1'
);
PREPARE grade_column_statement FROM @grade_column_sql;
EXECUTE grade_column_statement;
DEALLOCATE PREPARE grade_column_statement;

SET @performance_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'performance_salary'
);
SET @performance_column_sql = IF(
    @performance_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN performance_salary DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''月绩效工资'' AFTER salary_grade',
    'SELECT 1'
);
PREPARE performance_column_statement FROM @performance_column_sql;
EXECUTE performance_column_statement;
DEALLOCATE PREPARE performance_column_statement;

SET @deduction_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sys_user' AND COLUMN_NAME = 'deduction_salary'
);
SET @deduction_column_sql = IF(
    @deduction_column_exists = 0,
    'ALTER TABLE sys_user ADD COLUMN deduction_salary DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT ''月扣除工资'' AFTER performance_salary',
    'SELECT 1'
);
PREPARE deduction_column_statement FROM @deduction_column_sql;
EXECUTE deduction_column_statement;
DEALLOCATE PREPARE deduction_column_statement;

-- 根据原基础薪资就近初始化职级，随后统一为该职级的标准基础工资。
UPDATE sys_user
SET salary_grade = CASE
    WHEN salary >= 26500 THEN '20C' WHEN salary >= 23500 THEN '20B' WHEN salary >= 21000 THEN '20A'
    WHEN salary >= 19000 THEN '19C' WHEN salary >= 17250 THEN '19B' WHEN salary >= 15750 THEN '19A'
    WHEN salary >= 14500 THEN '18C' WHEN salary >= 13500 THEN '18B' WHEN salary >= 12500 THEN '18A'
    WHEN salary >= 11600 THEN '17C' WHEN salary >= 10850 THEN '17B' WHEN salary >= 10150 THEN '17A'
    WHEN salary >= 9500 THEN '16C' WHEN salary >= 8900 THEN '16B' WHEN salary >= 8300 THEN '16A'
    WHEN salary >= 7750 THEN '15C' WHEN salary >= 7250 THEN '15B' WHEN salary >= 6750 THEN '15A'
    WHEN salary >= 6250 THEN '14C' WHEN salary >= 5800 THEN '14B' WHEN salary >= 5400 THEN '14A'
    WHEN salary >= 5000 THEN '13C' WHEN salary >= 4650 THEN '13B' ELSE '13A'
END
WHERE deleted = 0;

UPDATE sys_user
SET salary = CASE salary_grade
    WHEN '13A' THEN 4500 WHEN '13B' THEN 4800 WHEN '13C' THEN 5200
    WHEN '14A' THEN 5600 WHEN '14B' THEN 6000 WHEN '14C' THEN 6500
    WHEN '15A' THEN 7000 WHEN '15B' THEN 7500 WHEN '15C' THEN 8000
    WHEN '16A' THEN 8600 WHEN '16B' THEN 9200 WHEN '16C' THEN 9800
    WHEN '17A' THEN 10500 WHEN '17B' THEN 11200 WHEN '17C' THEN 12000
    WHEN '18A' THEN 13000 WHEN '18B' THEN 14000 WHEN '18C' THEN 15000
    WHEN '19A' THEN 16500 WHEN '19B' THEN 18000 WHEN '19C' THEN 20000
    WHEN '20A' THEN 22000 WHEN '20B' THEN 25000 WHEN '20C' THEN 28000
    ELSE 4500
END
WHERE deleted = 0;

UPDATE sys_permission
SET name = '查看薪资管理', path = '/api/v1/salaries'
WHERE code = 'sys:salary:view';

UPDATE sys_permission
SET name = '维护员工薪资', path = '/api/v1/salaries/{userId}'
WHERE code = 'sys:salary:update';
