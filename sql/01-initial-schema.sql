USE oa_system;

CREATE TABLE IF NOT EXISTS sys_department (
    id BIGINT PRIMARY KEY COMMENT '部门主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父部门ID，0表示根部门',
    name VARCHAR(64) NOT NULL COMMENT '部门名称',
    manager_id BIGINT NULL COMMENT '部门负责人用户ID',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '部门排序号，数值越小越靠前',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    UNIQUE KEY uk_department_parent_name (parent_id, name),
    KEY idx_department_manager (manager_id)
) COMMENT='部门';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY COMMENT '用户主键',
    department_id BIGINT NULL COMMENT '所属部门ID',
    username VARCHAR(64) NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    display_name VARCHAR(64) NOT NULL COMMENT '用户显示名称',
    avatar_file_name VARCHAR(255) NULL COMMENT '本地头像文件名',
    phone VARCHAR(32) NULL COMMENT '手机号',
    email VARCHAR(128) NULL COMMENT '邮箱地址',
    salary DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '月基本薪资',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_department (department_id)
) COMMENT='用户';

CREATE TABLE IF NOT EXISTS sys_department_manager (
    department_id BIGINT NOT NULL COMMENT '部门ID',
    user_id BIGINT NOT NULL COMMENT '主管用户ID',
    is_primary TINYINT NOT NULL DEFAULT 0 COMMENT '是否主负责人：1是，0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '配置时间',
    PRIMARY KEY (department_id, user_id),
    KEY idx_department_manager_user (user_id, department_id)
) COMMENT='部门主管关联';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY COMMENT '角色主键',
    code VARCHAR(64) NOT NULL COMMENT '角色编码',
    name VARCHAR(64) NOT NULL COMMENT '角色名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    UNIQUE KEY uk_role_code (code)
) COMMENT='角色';

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT PRIMARY KEY COMMENT '权限主键',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父权限ID，0表示根节点',
    code VARCHAR(128) NOT NULL COMMENT '权限编码',
    name VARCHAR(64) NOT NULL COMMENT '权限名称',
    type VARCHAR(16) NOT NULL COMMENT '类型：menu或api',
    path VARCHAR(255) NULL COMMENT '菜单路径或接口路径',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    UNIQUE KEY uk_permission_code (code)
) COMMENT='菜单与权限';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关联';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    PRIMARY KEY (role_id, permission_id)
) COMMENT='角色权限关联';

CREATE TABLE IF NOT EXISTS attendance_record (
    id BIGINT PRIMARY KEY COMMENT '考勤记录主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    work_date DATE NOT NULL COMMENT '工作日期',
    check_in_time DATETIME NULL COMMENT '上班打卡时间',
    check_out_time DATETIME NULL COMMENT '下班打卡时间',
    actual_work_minutes INT NOT NULL DEFAULT 0 COMMENT '实际工时分钟数',
    status VARCHAR(32) NOT NULL COMMENT '考勤状态',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_attendance_user_date (user_id, work_date)
) COMMENT='考勤记录';

CREATE TABLE IF NOT EXISTS attendance_rule (
    id BIGINT PRIMARY KEY COMMENT '规则主键，当前固定为1',
    work_start TIME NOT NULL COMMENT '上班时间',
    work_end TIME NOT NULL COMMENT '下班时间',
    late_threshold_minutes INT NOT NULL DEFAULT 5 COMMENT '迟到宽限分钟数',
    updated_by BIGINT NULL COMMENT '最后修改人ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='当前考勤规则';

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

CREATE TABLE IF NOT EXISTS flow_request (
    id BIGINT PRIMARY KEY COMMENT '流程申请主键',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    request_type VARCHAR(32) NOT NULL COMMENT '申请类型：请假/加班',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    leave_type VARCHAR(32) NULL COMMENT '请假类型：PERSONAL/SICK/ANNUAL/COMPENSATORY',
    overtime_compensation VARCHAR(32) NULL COMMENT '加班补偿：PAY/COMPENSATORY',
    duration_minutes INT NOT NULL DEFAULT 0 COMMENT '申请时长（分钟）',
    status VARCHAR(32) NOT NULL COMMENT '流程状态',
    current_approver_id BIGINT NULL COMMENT '当前审批人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_flow_applicant_status (applicant_id, status),
    KEY idx_flow_applicant_time (applicant_id, start_time, end_time)
) COMMENT='请假与加班申请';

CREATE TABLE IF NOT EXISTS flow_action_log (
    id BIGINT PRIMARY KEY COMMENT '审批操作日志主键',
    request_id BIGINT NOT NULL COMMENT '流程申请ID',
    operator_id BIGINT NOT NULL COMMENT '审批人ID',
    action VARCHAR(32) NOT NULL COMMENT '流程动作：SUBMIT/APPROVE/REJECT/WITHDRAW',
    comment VARCHAR(500) NULL COMMENT '审批意见',
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_flow_action_request (request_id, operated_at),
    KEY idx_flow_action_operator (operator_id, operated_at)
) COMMENT='审批操作日志';

CREATE TABLE IF NOT EXISTS notice (
    id BIGINT PRIMARY KEY COMMENT '公告主键',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    summary VARCHAR(500) NULL COMMENT '公告摘要',
    content TEXT NOT NULL COMMENT '公告正文',
    publisher_id BIGINT NOT NULL COMMENT '发布人ID',
    status VARCHAR(32) NOT NULL COMMENT '公告状态：DRAFT/PUBLISHED/OFFLINE',
    top_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：0否，1是',
    published_at DATETIME NULL COMMENT '发布时间',
    offline_at DATETIME NULL COMMENT '下线时间',
    view_count INT NOT NULL DEFAULT 0 COMMENT '浏览次数',
    created_by BIGINT NULL COMMENT '创建人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT NULL COMMENT '更新人ID',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    KEY idx_notice_status_published (status, published_at),
    KEY idx_notice_top_published (top_flag, published_at)
) COMMENT='通知公告';

CREATE TABLE IF NOT EXISTS notice_read (
    notice_id BIGINT NOT NULL COMMENT '公告ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    PRIMARY KEY (notice_id, user_id)
) COMMENT='通知已读状态';

CREATE TABLE IF NOT EXISTS shared_document (
    id BIGINT PRIMARY KEY COMMENT '共享文档主键',
    department_id BIGINT NOT NULL COMMENT '所属部门ID',
    title VARCHAR(200) NOT NULL COMMENT '文档标题',
    content_json LONGTEXT NOT NULL COMMENT 'Tiptap JSON 文档内容',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    updated_by BIGINT NOT NULL COMMENT '最后编辑人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    KEY idx_shared_document_department_updated (department_id, updated_at),
    KEY idx_shared_document_updated_by (updated_by)
) COMMENT='部门共享文档';

CREATE TABLE IF NOT EXISTS office_supply (
    id BIGINT PRIMARY KEY COMMENT '办公用品主键',
    name VARCHAR(100) NOT NULL COMMENT '用品名称',
    category VARCHAR(64) NOT NULL COMMENT '用品分类',
    unit VARCHAR(32) NOT NULL COMMENT '计量单位',
    stock_quantity INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    safety_stock INT NOT NULL DEFAULT 0 COMMENT '安全库存',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1启用，0停用',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_office_supply_category_name (category, name),
    KEY idx_office_supply_status_stock (status, stock_quantity)
) COMMENT='办公用品库存';

CREATE TABLE IF NOT EXISTS office_supply_request (
    id BIGINT PRIMARY KEY COMMENT '办公用品申领主键',
    request_no VARCHAR(64) NOT NULL COMMENT '申领单号',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    department_id BIGINT NOT NULL COMMENT '申请人部门ID',
    supply_id BIGINT NOT NULL COMMENT '用品ID',
    quantity INT NOT NULL COMMENT '申请数量',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    status VARCHAR(32) NOT NULL COMMENT 'PENDING/APPROVED/REJECTED/ISSUED/CANCELLED',
    reviewer_id BIGINT NULL COMMENT '审批人ID',
    review_comment VARCHAR(500) NULL COMMENT '审批意见',
    reviewed_at DATETIME NULL COMMENT '审批时间',
    issued_by BIGINT NULL COMMENT '发放人ID',
    issued_at DATETIME NULL COMMENT '发放时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_supply_request_no (request_no),
    KEY idx_supply_request_applicant (applicant_id, created_at),
    KEY idx_supply_request_department_status (department_id, status, created_at)
) COMMENT='办公用品申领单';

CREATE TABLE IF NOT EXISTS fixed_asset (
    id BIGINT PRIMARY KEY COMMENT '固定资产主键',
    asset_code VARCHAR(64) NOT NULL COMMENT '资产编号',
    name VARCHAR(100) NOT NULL COMMENT '资产名称',
    category VARCHAR(64) NOT NULL COMMENT '资产分类',
    specification VARCHAR(255) NULL COMMENT '品牌型号或规格',
    purchase_date DATE NULL COMMENT '购置日期',
    original_value DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '资产原值',
    status VARCHAR(32) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE/IN_USE/REPAIR/SCRAPPED',
    custodian_id BIGINT NULL COMMENT '当前保管人ID',
    department_id BIGINT NULL COMMENT '当前使用部门ID',
    location VARCHAR(128) NULL COMMENT '存放地点',
    remark VARCHAR(500) NULL COMMENT '备注',
    created_by BIGINT NULL COMMENT '创建人ID',
    updated_by BIGINT NULL COMMENT '更新人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE KEY uk_fixed_asset_code (asset_code),
    KEY idx_fixed_asset_department_status (department_id, status),
    KEY idx_fixed_asset_custodian (custodian_id, status)
) COMMENT='固定资产台账';
