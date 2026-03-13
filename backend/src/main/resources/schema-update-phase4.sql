-- Create user common certificate table.
CREATE TABLE IF NOT EXISTS user_certificate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    certificate_type VARCHAR(50) NOT NULL,
    trademark_type VARCHAR(50) NULL DEFAULT '未知',
    trademark_content VARCHAR(255) NOT NULL,
    principal VARCHAR(255) NOT NULL,
    end_date DATE NOT NULL,
    file_urls TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
