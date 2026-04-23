-- V11__add_point_event_target_fields.sql
-- 포인트 이벤트 선택 적용 옵션 필드 추가

ALTER TABLE event
    ADD COLUMN IF NOT EXISTS point_event_target_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS point_event_target_value VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS point_event_min_order_amount BIGINT;
