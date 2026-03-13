CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(100),
    company_name VARCHAR(100),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    email VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_app_user_phone (phone),
    UNIQUE KEY uk_app_user_email (email)
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT NULL,
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    image_urls TEXT,
    base_price DECIMAL(10, 2) NOT NULL,
    pricing_formula TEXT,
    category_id BIGINT,
    setup_fee DECIMAL(10, 2) DEFAULT 0.00,
    free_setup_quantity INT DEFAULT 0,
    status INT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS product_parameters (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    param_name VARCHAR(100) NOT NULL,
    param_type VARCHAR(20) NOT NULL DEFAULT 'INPUT',
    is_required BOOLEAN DEFAULT TRUE,
    is_multiple BOOLEAN DEFAULT FALSE,
    is_dynamic_by_material BOOLEAN DEFAULT FALSE,
    unit VARCHAR(20) DEFAULT NULL,
    pricing_rule TEXT,
    validation_rules TEXT,
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS parameter_options (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parameter_id BIGINT NOT NULL,
    option_name VARCHAR(100) NOT NULL,
    price_adjustment DECIMAL(10, 2) DEFAULT 0.00,
    image_url VARCHAR(500),
    sort_order INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS product_material_param_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    material_option_id BIGINT NOT NULL,
    param_id BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_material_param (product_id, material_option_id, param_id)
);

CREATE TABLE IF NOT EXISTS product_material_param_option_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    material_option_id BIGINT NOT NULL,
    param_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_product_material_param_option (product_id, material_option_id, param_id, option_id)
);

CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    params_snapshot TEXT,
    unit_price DECIMAL(10, 2) NOT NULL,
    design_file_url VARCHAR(500),
    proof_file_url VARCHAR(500),
    has_copyright BOOLEAN DEFAULT FALSE,
    copyright_file_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    total_amount DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    custom_name VARCHAR(100),
    remark VARCHAR(500),
    receiver_name VARCHAR(50),
    receiver_phone VARCHAR(20),
    receiver_address VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    params_snapshot TEXT,
    design_file_url VARCHAR(500),
    proof_file_url VARCHAR(500),
    has_copyright BOOLEAN DEFAULT FALSE,
    copyright_file_url VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    receiver_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    province VARCHAR(50) NOT NULL,
    city VARCHAR(50) NOT NULL,
    district VARCHAR(50) NOT NULL,
    detail_address VARCHAR(255) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

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
