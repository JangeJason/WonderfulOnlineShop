ALTER TABLE product
  ADD COLUMN image_urls TEXT NULL COMMENT 'Product image URLs in JSON array format' AFTER image_url;
