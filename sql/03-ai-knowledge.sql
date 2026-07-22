USE oa_system;

CREATE TABLE IF NOT EXISTS ai_knowledge_doc (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    doc_title VARCHAR(200) NOT NULL COMMENT '文档标题',
    doc_domain VARCHAR(32) NOT NULL COMMENT '知识域：ATTENDANCE/FLOW/HR',
    doc_version VARCHAR(32) NOT NULL COMMENT '文档版本',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件存储地址',
    content_hash VARCHAR(64) NOT NULL COMMENT '文档内容哈希，用于幂等控制',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '状态：DRAFT/APPROVED/RETIRED',
    source_type VARCHAR(20) NOT NULL DEFAULT 'UPLOAD' COMMENT '来源：UPLOAD/MANUAL/SEED',
    effective_date DATE DEFAULT NULL COMMENT '生效日期',
    approved_by BIGINT DEFAULT NULL COMMENT '审核人ID',
    approved_at DATETIME(3) DEFAULT NULL COMMENT '审核时间',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_by BIGINT DEFAULT NULL COMMENT '更新人ID',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记：0未删除，1已删除',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_ai_knowledge_doc_content_hash (content_hash),
    KEY idx_ai_knowledge_doc_domain_status (doc_domain, status),
    KEY idx_ai_knowledge_doc_title (doc_title),
    KEY idx_ai_knowledge_doc_created_at (created_at)
) COMMENT='AI知识文档表';

CREATE TABLE IF NOT EXISTS ai_knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    doc_id BIGINT NOT NULL COMMENT '所属文档ID',
    chunk_no INT NOT NULL COMMENT '片段序号，从1开始',
    chunk_title VARCHAR(200) DEFAULT NULL COMMENT '片段标题',
    chunk_text TEXT NOT NULL COMMENT '片段正文',
    chunk_hash VARCHAR(64) NOT NULL COMMENT '片段哈希，用于去重',
    vector_key VARCHAR(255) DEFAULT NULL COMMENT 'Redis向量索引key',
    metadata_json JSON DEFAULT NULL COMMENT '元数据快照',
    embedding_model VARCHAR(100) DEFAULT NULL COMMENT '向量模型名',
    embedding_dim INT DEFAULT NULL COMMENT '向量维度',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    UNIQUE KEY uk_ai_knowledge_chunk_doc_no (doc_id, chunk_no),
    UNIQUE KEY uk_ai_knowledge_chunk_hash (chunk_hash),
    KEY idx_ai_knowledge_chunk_doc_id (doc_id),
    CONSTRAINT fk_ai_knowledge_chunk_doc_id FOREIGN KEY (doc_id) REFERENCES ai_knowledge_doc (id)
) COMMENT='AI知识片段表';

CREATE TABLE IF NOT EXISTS ai_chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_no VARCHAR(64) NOT NULL COMMENT '会话编号',
    user_id BIGINT NOT NULL COMMENT '提问用户ID',
    session_title VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    knowledge_domain VARCHAR(32) DEFAULT 'ALL' COMMENT '知识域：ALL/ATTENDANCE/FLOW/HR',
    latest_question VARCHAR(1000) DEFAULT NULL COMMENT '最近一次问题',
    latest_answer TEXT DEFAULT NULL COMMENT '最近一次答案摘要',
    message_count INT NOT NULL DEFAULT 0 COMMENT '消息条数',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/ARCHIVED',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_ai_chat_session_no (session_no),
    KEY idx_ai_chat_session_user_id (user_id),
    KEY idx_ai_chat_session_updated_at (updated_at)
) COMMENT='AI问答会话表';

CREATE TABLE IF NOT EXISTS ai_chat_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    session_id BIGINT DEFAULT NULL COMMENT '所属会话ID',
    user_id BIGINT NOT NULL COMMENT '提问用户ID',
    question VARCHAR(2000) NOT NULL COMMENT '用户问题',
    answer TEXT NOT NULL COMMENT '模型回答',
    retrieved_doc_ids VARCHAR(1000) DEFAULT NULL COMMENT '命中文档ID列表',
    retrieved_chunk_ids VARCHAR(1000) DEFAULT NULL COMMENT '命中片段ID列表',
    citations_json JSON DEFAULT NULL COMMENT '引用片段JSON',
    model_name VARCHAR(100) NOT NULL COMMENT '模型名称',
    top_k INT NOT NULL DEFAULT 3 COMMENT '召回条数',
    confidence_score DECIMAL(5,2) DEFAULT NULL COMMENT '置信分',
    hit_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否命中知识库：0否，1是',
    latency_ms INT DEFAULT NULL COMMENT '耗时毫秒',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    KEY idx_ai_chat_log_user_id (user_id),
    KEY idx_ai_chat_log_session_id (session_id),
    KEY idx_ai_chat_log_created_at (created_at),
    CONSTRAINT fk_ai_chat_log_session_id FOREIGN KEY (session_id) REFERENCES ai_chat_session (id)
) COMMENT='AI问答记录表';

CREATE TABLE IF NOT EXISTS ai_index_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    task_no VARCHAR(64) NOT NULL COMMENT '任务编号',
    doc_id BIGINT DEFAULT NULL COMMENT '关联文档ID',
    task_type VARCHAR(20) NOT NULL COMMENT '任务类型：INGEST/REINDEX/DELETE_INDEX',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/RUNNING/SUCCESS/FAILED',
    error_message VARCHAR(1000) DEFAULT NULL COMMENT '失败原因',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    created_by BIGINT DEFAULT NULL COMMENT '创建人ID',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    UNIQUE KEY uk_ai_index_task_no (task_no),
    KEY idx_ai_index_task_doc_id (doc_id),
    KEY idx_ai_index_task_status (status),
    CONSTRAINT fk_ai_index_task_doc_id FOREIGN KEY (doc_id) REFERENCES ai_knowledge_doc (id)
) COMMENT='AI索引任务表';
