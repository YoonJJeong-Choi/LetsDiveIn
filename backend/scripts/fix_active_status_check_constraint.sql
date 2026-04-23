-- ActiveStatus enum 변경: DELETE -> INACTIVE
-- product와 option 테이블의 체크 제약 조건 수정 및 기존 데이터 업데이트

-- 제약 조건 이름 확인 (실행 전 확인용)
-- SELECT constraint_name, constraint_type 
-- FROM information_schema.table_constraints 
-- WHERE table_name = 'product' AND constraint_type = 'CHECK';
-- 
-- SELECT constraint_name, constraint_type 
-- FROM information_schema.table_constraints 
-- WHERE table_name = 'option' AND constraint_type = 'CHECK';

-- 1. product 테이블: 기존 DELETE 값을 INACTIVE로 변경
UPDATE product 
SET product_active_status = 'INACTIVE' 
WHERE product_active_status = 'DELETE';

-- 2. option 테이블: 기존 DELETE 값을 INACTIVE로 변경
UPDATE "option" 
SET option_status = 'INACTIVE' 
WHERE option_status = 'DELETE';

-- 3. product 테이블의 체크 제약 조건 삭제 및 재생성
-- 기존 제약 조건 삭제 (제약 조건 이름이 다를 수 있으므로 확인 필요)
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_product_active_status_check;
ALTER TABLE product DROP CONSTRAINT IF EXISTS product_active_status_check;

-- 새로운 제약 조건 추가 (ACTIVE, INACTIVE만 허용)
ALTER TABLE product 
ADD CONSTRAINT product_product_active_status_check 
CHECK (product_active_status IN ('ACTIVE', 'INACTIVE'));

-- 4. option 테이블의 체크 제약 조건 삭제 및 재생성
-- 기존 제약 조건 삭제 (제약 조건 이름이 다를 수 있으므로 확인 필요)
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_option_status_check;
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_status_check;

-- 새로운 제약 조건 추가 (ACTIVE, INACTIVE만 허용)
ALTER TABLE "option" 
ADD CONSTRAINT option_option_status_check 
CHECK (option_status IN ('ACTIVE', 'INACTIVE'));
