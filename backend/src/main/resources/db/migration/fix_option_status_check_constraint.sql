-- option 테이블의 체크 제약 조건에 PENDING_UPDATE 추가
-- 이 파일은 수동으로 실행하거나 Flyway가 자동으로 실행합니다.

-- 기존 제약 조건 삭제
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_option_status_check;
ALTER TABLE "option" DROP CONSTRAINT IF EXISTS option_status_check;

-- 새로운 제약 조건 추가 (PENDING, ACTIVE, REJECTED, INACTIVE, PENDING_UPDATE 모두 허용)
ALTER TABLE "option" 
ADD CONSTRAINT option_option_status_check 
CHECK (option_status IN ('PENDING', 'ACTIVE', 'REJECTED', 'INACTIVE', 'PENDING_UPDATE'));
