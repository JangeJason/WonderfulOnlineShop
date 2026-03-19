CREATE TABLE IF NOT EXISTS product_config_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(16) NOT NULL,
    creator_user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    params_snapshot TEXT NOT NULL,
    expire_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_config_code_code (code),
    KEY idx_product_config_code_creator_created (creator_user_id, created_at),
    KEY idx_product_config_code_product (product_id)
);

