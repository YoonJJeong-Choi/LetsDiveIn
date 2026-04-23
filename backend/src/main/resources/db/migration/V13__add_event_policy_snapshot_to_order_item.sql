-- 주문 아이템에 이벤트 정책 스냅샷 컬럼 추가
-- JSON 문자열로 저장 (DB 호환성을 위해 TEXT로 생성)
ALTER TABLE order_item
ADD COLUMN IF NOT EXISTS admin_event_policy_snapshot TEXT NULL;

ALTER TABLE order_item
ADD COLUMN IF NOT EXISTS partner_event_policy_snapshot TEXT NULL;

