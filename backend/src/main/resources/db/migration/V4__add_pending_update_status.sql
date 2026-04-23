-- ActiveStatus enum에 PENDING_UPDATE 추가
-- product와 option 테이블의 체크 제약 조건에 PENDING_UPDATE 추가

-- 1. product 테이블의 체크 제약 조건 수정 (PENDING_UPDATE 추가)
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_product_active_status_check;
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_active_status_check;

-- 새로운 제약 조건 추가 (PENDING, ACTIVE, REJECTED, INACTIVE, PENDING_UPDATE 모두 허용)
ALTER TABLE product 
ADD CONSTRAINT product_product_active_status_check 
CHECK (product_active_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE', 'PENDING_UPDATE'));

-- 2. option 테이블의 체크 제약 조건 수정 (PENDING_UPDATE 추가)
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_option_status_check;
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_status_check;

-- 새로운 제약 조건 추가 (PENDING, ACTIVE, REJECTED, INACTIVE, PENDING_UPDATE 모두 허용)
ALTER TABLE "option" 
ADD CONSTRAINT option_option_status_check 
CHECK (option_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE', 'PENDING_UPDATE'));
