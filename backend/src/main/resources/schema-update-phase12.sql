-- Phase 12: production sub-status and review fields

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'production_status'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN production_status VARCHAR(30) NULL COMMENT ''生产子状态''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'review_status'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN review_status VARCHAR(20) NULL DEFAULT ''PENDING'' COMMENT ''审核状态''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'review_reason'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN review_reason VARCHAR(500) NULL COMMENT ''驳回原因''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'reviewed_by'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN reviewed_by BIGINT NULL COMMENT ''审核人''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'reviewed_at'
);
SET @sql := IF(@col_exists = 0, 'ALTER TABLE orders ADD COLUMN reviewed_at TIMESTAMP NULL COMMENT ''审核时间''', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE orders
SET review_status = CASE
    WHEN status = 'REJECTED' THEN 'REJECTED'
    WHEN status IN ('IN_PRODUCTION', 'WAIT_SHIPMENT', 'SHIPPED', 'COMPLETED', 'AFTER_SALE') THEN 'APPROVED'
    ELSE 'PENDING'
  END
WHERE review_status IS NULL OR review_status = '';

UPDATE orders
SET production_status = 'PREPRESS_CHECK'
WHERE status = 'IN_PRODUCTION'
  AND (production_status IS NULL OR production_status = '');

UPDATE orders
SET shipping_status = 'IN_TRANSIT'
WHERE status = 'SHIPPED'
  AND (shipping_status IS NULL OR shipping_status = '');
