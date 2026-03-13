-- Add validation_rules column to product_parameters table
ALTER TABLE product_parameters ADD COLUMN validation_rules JSON COMMENT 'Validation Rules (e.g., min, max, regex)';
