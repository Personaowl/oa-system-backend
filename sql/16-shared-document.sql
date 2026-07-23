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
