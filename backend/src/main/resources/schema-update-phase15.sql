SET @col_exists := (
  SELECT COUNT(1)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'after_sale_request'
    AND COLUMN_NAME = 'user_deleted'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE after_sale_request ADD COLUMN user_deleted BOOLEAN NOT NULL DEFAULT FALSE COMMENT ''用户端逻辑删除标记'' AFTER admin_remark',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
