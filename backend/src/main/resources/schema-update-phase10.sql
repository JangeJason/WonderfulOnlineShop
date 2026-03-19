-- Phase 10: order status flow upgrade (shipping sub-status)
-- Compatible with MySQL versions that do not support "ADD COLUMN IF NOT EXISTS"

SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'orders'
    AND COLUMN_NAME = 'shipping_status'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE orders ADD COLUMN shipping_status VARCHAR(30) NULL COMMENT ''物流子状态: IN_TRANSIT/OUT_FOR_DELIVERY/SIGNED''',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
