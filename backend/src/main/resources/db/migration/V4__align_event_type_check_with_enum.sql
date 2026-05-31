-- Java EventType(SALE, POINT, NOTICE)와 DB CHECK 제약 정합
-- 기존 V1: COUPON, GIFT (미사용) → NOTICE 반영

ALTER TABLE event DROP CONSTRAINT IF EXISTS event_event_type_check;
ALTER TABLE event ADD CONSTRAINT event_event_type_check
    CHECK (
        event_type IS NULL
        OR event_type IN ('SALE', 'POINT', 'NOTICE')
    );

ALTER TABLE order_item DROP CONSTRAINT IF EXISTS order_item_applied_sale_event_type_check;
ALTER TABLE order_item ADD CONSTRAINT order_item_applied_sale_event_type_check
    CHECK (
        applied_sale_event_type IS NULL
        OR applied_sale_event_type IN ('SALE', 'POINT', 'NOTICE')
    );
