-- 강제형 시즌 SALE 이벤트 할인 정책 필드
ALTER TABLE event
    ADD COLUMN IF NOT EXISTS sale_discount_type VARCHAR(20),
    ADD COLUMN IF NOT EXISTS sale_discount_value BIGINT,
    ADD COLUMN IF NOT EXISTS sale_max_discount_amount BIGINT;

