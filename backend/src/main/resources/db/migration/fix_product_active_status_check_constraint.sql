-- ActiveStatus enum에 PENDING, REJECTED 추가
-- product와 option 테이블의 체크 제약 조건 수정

-- 1. product 테이블의 체크 제약 조건 삭제 및 재생성
-- 기존 제약 조건 삭제 (제약 조건 이름이 다를 수 있으므로 확인 필요)
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_product_active_status_check;
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_active_status_check;

-- 새로운 제약 조건 추가 (PENDING, ACTIVE, REJECTED, INACTIVE 모두 허용)
ALTER TABLE product 
ADD CONSTRAINT product_product_active_status_check 
CHECK (product_active_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE'));

-- 2. option 테이블의 체크 제약 조건 삭제 및 재생성
-- 기존 제약 조건 삭제 (제약 조건 이름이 다를 수 있으므로 확인 필요)
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_option_status_check;
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_status_check;

-- 새로운 제약 조건 추가 (PENDING, ACTIVE, REJECTED, INACTIVE 모두 허용)
ALTER TABLE "option" 
ADD CONSTRAINT option_option_status_check 
CHECK (option_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE'));
