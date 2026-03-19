CREATE TABLE IF NOT EXISTS after_sale_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    item_ids_json TEXT NOT NULL,
    request_type VARCHAR(40) NOT NULL,
    detail_text TEXT,
    image_urls TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    admin_remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_after_sale_order (order_id),
    KEY idx_after_sale_user_created (user_id, created_at),
    KEY idx_after_sale_status_created (status, created_at)
);
