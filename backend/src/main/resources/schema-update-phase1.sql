-- ----------------------------
-- Table structure for product_parameters
-- ----------------------------
DROP TABLE IF EXISTS `product_parameters`;
CREATE TABLE `product_parameters` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Parameter ID',
  `product_id` bigint NOT NULL COMMENT 'Associated Product ID',
  `param_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Parameter Name (e.g., Length)',
  `param_type` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Parameter Type (INPUT, SELECT)',
  `is_required` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'Is Required',
  `pricing_rule` json DEFAULT NULL COMMENT 'Pricing Rule (JSON)',
  `sort_order` int DEFAULT '0' COMMENT 'Sort Order',
  PRIMARY KEY (`id`),
  KEY `idx_product_id` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Product Parameters Definition';

-- ----------------------------
-- Table structure for parameter_options
-- ----------------------------
DROP TABLE IF EXISTS `parameter_options`;
CREATE TABLE `parameter_options` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Option ID',
  `parameter_id` bigint NOT NULL COMMENT 'Associated Parameter ID',
  `option_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Option Name (e.g., Oak)',
  `price_adjustment` decimal(10,2) DEFAULT '0.00' COMMENT 'Price Adjustment',
  `sort_order` int DEFAULT '0' COMMENT 'Sort Order',
  PRIMARY KEY (`id`),
  KEY `idx_parameter_id` (`parameter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Parameter Options';
