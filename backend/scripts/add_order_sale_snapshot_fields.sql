-- 주문 스냅샷 필드 추가
-- 1) orders.used_point_amount
-- 2) order_item.applied_sale_policy_no / applied_sale_campaign_id / sale_evaluated_at

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS used_point_amount BIGINT NOT NULL DEFAULT 0;

ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS applied_sale_policy_no BIGINT,
    ADD COLUMN IF NOT EXISTS applied_sale_campaign_id VARCHAR(100),
    ADD COLUMN IF NOT EXISTS sale_evaluated_at TIMESTAMP;
