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

CREATE TABLE IF NOT EXISTS flow_request (
    id BIGINT PRIMARY KEY COMMENT '流程申请主键',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID',
    request_type VARCHAR(32) NOT NULL COMMENT '申请类型：请假/加班',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NOT NULL COMMENT '结束时间',
    reason VARCHAR(500) NOT NULL COMMENT '申请原因',
    status VARCHAR(32) NOT NULL COMMENT '流程状态',
    current_approver_id BIGINT NULL COMMENT '当前审批人ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_flow_applicant_status (applicant_id, status)
) COMMENT='请假与加班申请';

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
