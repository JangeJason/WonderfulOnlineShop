ALTER TABLE product_parameters
  ADD COLUMN is_dynamic_by_material BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether this parameter is controlled by material rules' AFTER is_multiple;

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
