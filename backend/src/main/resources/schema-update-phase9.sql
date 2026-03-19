CREATE TABLE IF NOT EXISTS user_favorite_product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_favorite_product (user_id, product_id),
    KEY idx_user_favorite_product_user_created (user_id, created_at)
);

CREATE TABLE IF NOT EXISTS user_favorite_config_code (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    config_code_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_favorite_config_code (user_id, config_code_id),
    KEY idx_user_favorite_config_code_user_created (user_id, created_at),
    KEY idx_user_favorite_config_code_product (product_id)
);
