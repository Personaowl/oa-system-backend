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

INSERT INTO office_supply
    (id, name, category, unit, stock_quantity, safety_stock, status, created_at, updated_at, version, deleted)
VALUES
    (170001, '中性笔', '书写用品', '支', 120, 30, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (170002, 'A4打印纸', '纸张耗材', '包', 48, 15, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (170003, '便利贴', '桌面文具', '本', 65, 20, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (170004, '文件夹', '文件管理', '个', 36, 10, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (170005, '硒鼓', '打印耗材', '个', 8, 5, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE
    unit = VALUES(unit), safety_stock = VALUES(safety_stock), status = VALUES(status), deleted = 0;

INSERT INTO fixed_asset
    (id, asset_code, name, category, specification, purchase_date, original_value, status,
     custodian_id, department_id, location, remark, created_at, updated_at, version, deleted)
VALUES
    (180001, 'PC-2026-001', '联想ThinkBook笔记本', '电脑设备', 'ThinkBook 16 / 32GB / 1TB', '2026-01-10', 6799.00, 'IDLE', NULL, NULL, '总部资产库', '演示资产', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (180002, 'MON-2026-001', '戴尔27寸显示器', '显示设备', 'Dell P2723D', '2026-01-10', 2199.00, 'IDLE', NULL, NULL, '总部资产库', '演示资产', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0),
    (180003, 'PRN-2026-001', '惠普激光打印机', '办公设备', 'HP LaserJet Pro', '2026-02-18', 3299.00, 'IDLE', NULL, NULL, '总部资产库', '演示资产', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), specification = VALUES(specification), deleted = 0;
