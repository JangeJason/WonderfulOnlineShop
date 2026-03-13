-- Add proof file columns for cart/order items (MySQL-compatible idempotent script).
SET @db_name = DATABASE();

SET @cart_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'cart_item'
      AND COLUMN_NAME = 'proof_file_url'
);
SET @cart_sql = IF(
    @cart_col_exists = 0,
    'ALTER TABLE cart_item ADD COLUMN proof_file_url VARCHAR(500) NULL COMMENT ''工艺对稿图文件URL''',
    'SELECT ''cart_item.proof_file_url already exists'' AS msg'
);
PREPARE cart_stmt FROM @cart_sql;
EXECUTE cart_stmt;
DEALLOCATE PREPARE cart_stmt;

SET @order_col_exists = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'order_item'
      AND COLUMN_NAME = 'proof_file_url'
);
SET @order_sql = IF(
    @order_col_exists = 0,
    'ALTER TABLE order_item ADD COLUMN proof_file_url VARCHAR(500) NULL COMMENT ''工艺对稿图文件URL''',
    'SELECT ''order_item.proof_file_url already exists'' AS msg'
);
PREPARE order_stmt FROM @order_sql;
EXECUTE order_stmt;
DEALLOCATE PREPARE order_stmt;
