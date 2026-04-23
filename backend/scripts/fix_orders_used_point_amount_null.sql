-- orders.used_point_amount NULL 데이터 보정 + 제약 재설정
-- 배포 순서:
-- 1) 본 스크립트 실행
-- 2) 애플리케이션 재기동 후 주문 생성/조회 점검

BEGIN;

-- 1) 기존 NULL 데이터 백필
UPDATE orders
SET used_point_amount = 0
WHERE used_point_amount IS NULL;

-- 2) 기본값 보장
ALTER TABLE orders
    ALTER COLUMN used_point_amount SET DEFAULT 0;

-- 3) NOT NULL 보장
ALTER TABLE orders
    ALTER COLUMN used_point_amount SET NOT NULL;

COMMIT;

