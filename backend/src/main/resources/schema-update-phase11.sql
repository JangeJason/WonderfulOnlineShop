-- Phase 11: migrate legacy order status from PAID to WAIT_PRODUCTION

UPDATE orders
SET status = 'WAIT_PRODUCTION',
    updated_at = CURRENT_TIMESTAMP
WHERE status = 'PAID';

