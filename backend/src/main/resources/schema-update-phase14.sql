CREATE TABLE IF NOT EXISTS admin_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_user_id BIGINT NOT NULL,
    admin_nickname VARCHAR(100),
    admin_company_name VARCHAR(100),
    module_name VARCHAR(50) NOT NULL,
    action_name VARCHAR(50) NOT NULL,
    http_method VARCHAR(10) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    target_id BIGINT NULL,
    summary VARCHAR(500),
    ip_address VARCHAR(64),
    user_agent VARCHAR(300),
    status_code INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    KEY idx_admin_oplog_admin_created (admin_user_id, created_at),
    KEY idx_admin_oplog_module_created (module_name, created_at),
    KEY idx_admin_oplog_created (created_at)
);
