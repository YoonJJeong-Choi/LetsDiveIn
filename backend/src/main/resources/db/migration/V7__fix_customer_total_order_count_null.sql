-- 고객 테이블의 total_order_count와 total_purchase_amount null 값 처리
-- 기존 데이터의 null 값을 0으로 업데이트

UPDATE customer 
SET total_order_count = 0 
WHERE total_order_count IS NULL;

UPDATE customer 
SET total_purchase_amount = 0 
WHERE total_purchase_amount IS NULL;

-- NOT NULL 제약조건이 없으면 추가 (이미 있으면 에러 발생하지만 무시 가능)
-- ALTER TABLE customer ALTER COLUMN total_order_count SET NOT NULL;
-- ALTER TABLE customer ALTER COLUMN total_purchase_amount SET NOT NULL;

-- 기본값 설정 (이미 있으면 에러 발생하지만 무시 가능)
-- ALTER TABLE customer ALTER COLUMN total_order_count SET DEFAULT 0;
-- ALTER TABLE customer ALTER COLUMN total_purchase_amount SET DEFAULT 0;
