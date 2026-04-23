-- Partner 테이블의 business_registration_number null 값 처리 스크립트

-- 방법 1: 기존 데이터에 임시 사업자등록번호 설정 (개발 환경)
UPDATE partner 
SET business_registration_number = '000-00-00000' 
WHERE business_registration_number IS NULL;

-- 방법 2: 기존 데이터 삭제 (개발 환경, 데이터가 중요하지 않은 경우)
-- DELETE FROM partner WHERE business_registration_number IS NULL;

-- 방법 3: 컬럼을 nullable로 변경 (비추천 - 비즈니스 로직상 사업자등록번호는 필수)
-- ALTER TABLE partner ALTER COLUMN business_registration_number DROP NOT NULL;
