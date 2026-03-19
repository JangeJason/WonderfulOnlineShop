ALTER TABLE orders
    ADD COLUMN out_trade_no VARCHAR(64) NULL AFTER receiver_address,
    ADD COLUMN pay_channel VARCHAR(20) NULL AFTER out_trade_no,
    ADD COLUMN paid_at TIMESTAMP NULL AFTER pay_channel;

ALTER TABLE orders
    ADD UNIQUE KEY uk_orders_out_trade_no (out_trade_no);
