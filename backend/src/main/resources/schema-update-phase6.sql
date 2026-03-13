ALTER TABLE product_parameters
  ADD COLUMN is_multiple BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Whether select parameter supports multi-select' AFTER is_required;
