-- NOTE:
-- This file is kept as a manual seed script.
-- Current backend default is spring.sql.init.mode=never, so this script is not auto-executed.

-- Sample Categories
INSERT INTO category (id, name, sort_order) VALUES
    (1, '名片印制', 1),
    (2, '宣传单/海报', 2),
    (3, '画册/宣传册', 3),
    (4, '不干胶/贴纸', 4),
    (5, '包装周边', 5);

-- Sample Products
-- pricing_formula uses SpEL variable syntax: #<param_name>
INSERT INTO product (id, name, description, image_url, base_price, pricing_formula, category_id, setup_fee, free_setup_quantity, status) VALUES
    (1, '高档铜版纸名片', '300g高档铜版纸，双面覆亚膜，手感细腻，彰显商务风范。', '/uploads/sample-business-card.jpg', 15.00, '#QTY * 0.08', 1, 80.00, 10, 1),
    (2, '特种纸艺术名片', '多种进口特种纸可选，适合设计师、艺术工作者。', null, 35.00, '#QTY * 0.12', 1, 120.00, 10, 1),
    (3, 'A4 宣传单页 (铜版纸)', '157g铜版纸，适合派发与展会宣传。', null, 50.00, '#QTY * 0.03', 2, 0.00, 0, 1),
    (4, '硬壳精装企业画册', '封面裱灰板，内页锁线胶装。', null, 800.00, '#PAGES * 1.20', 3, 0.00, 0, 1),
    (5, 'PVC防水不干胶贴纸', '防水防油防撕，适合包装标签。', null, 30.00, '#AREA * 6.50', 4, 0.00, 0, 1),
    (6, '定制手提纸袋', '白卡纸覆膜，穿棉绳。', null, 150.00, '#QTY * 0.05', 5, 100.00, 20, 1);

-- Product 1 Parameters
INSERT INTO product_parameters (id, product_id, param_name, param_type, is_required, unit, sort_order) VALUES
    (1, 1, 'QTY', 'INPUT', TRUE, '盒', 1),
    (2, 1, '材质', 'SELECT', TRUE, NULL, 2),
    (3, 1, '印后工艺', 'SELECT', FALSE, NULL, 3);

INSERT INTO parameter_options (id, parameter_id, option_name, price_adjustment, image_url, sort_order) VALUES
    (1, 2, '300g 铜版纸', 0.00, NULL, 1),
    (2, 2, '300g 哑粉纸', 5.00, NULL, 2),
    (3, 2, '350g 铜版纸（加厚）', 10.00, NULL, 3),
    (4, 3, '双面覆亚膜', 5.00, NULL, 1),
    (5, 3, '双面覆光膜', 5.00, NULL, 2),
    (6, 3, '单面局部UV', 15.00, NULL, 3),
    (7, 3, '烫金/烫银', 25.00, NULL, 4);

-- Product 3 Parameters
INSERT INTO product_parameters (id, product_id, param_name, param_type, is_required, unit, sort_order) VALUES
    (4, 3, 'QTY', 'INPUT', TRUE, '份', 1),
    (5, 3, '纸张厚度', 'SELECT', TRUE, NULL, 2),
    (6, 3, '折页方式', 'SELECT', FALSE, NULL, 3);

INSERT INTO parameter_options (id, parameter_id, option_name, price_adjustment, image_url, sort_order) VALUES
    (8, 5, '105g 铜版纸', -10.00, NULL, 1),
    (9, 5, '157g 铜版纸', 0.00, NULL, 2),
    (10, 5, '200g 铜版纸', 20.00, NULL, 3),
    (11, 6, '不折页（平张）', 0.00, NULL, 1),
    (12, 6, '对折', 15.00, NULL, 2),
    (13, 6, '荷包折/风琴折（三折）', 25.00, NULL, 3);

-- Product 4/5/6 Input Parameters for formulas
INSERT INTO product_parameters (id, product_id, param_name, param_type, is_required, unit, sort_order) VALUES
    (7, 4, 'PAGES', 'INPUT', TRUE, '页', 1),
    (8, 5, 'AREA', 'INPUT', TRUE, '平方米', 1),
    (9, 6, 'QTY', 'INPUT', TRUE, '个', 1);

-- Sample Users (password plaintext: admin)
INSERT INTO app_user (id, username, password, nickname, company_name, avatar_url, phone, email, role) VALUES
    (1, 'admin', '$2a$10$lN9bf8xNUKKB.lBaBXtbSOj8cRV9k8Uf5U2GpwhiCyfbqFlCeVcAC', '系统管理员', '环地福印刷科技', NULL, '13800000000', 'admin@wonderful.com', 'ADMIN'),
    (2, 'user', '$2a$10$lN9bf8xNUKKB.lBaBXtbSOj8cRV9k8Uf5U2GpwhiCyfbqFlCeVcAC', '张经理', '大鱼文化传媒有限公司', NULL, '13900000000', 'user@wonderful.com', 'USER');
