-- Partner 관련 테이블 정리 스크립트
-- 주의: 이 스크립트는 개발 환경에서만 사용하세요. 모든 데이터가 삭제됩니다.

-- 1. option 테이블의 partner 참조 데이터 삭제 (또는 테이블 전체 삭제)
-- 옵션: option 테이블의 데이터만 삭제
-- DELETE FROM option;

-- 옵션: option 테이블 전체 삭제 (더 확실함)
DROP TABLE IF EXISTS option CASCADE;

-- 2. settlement 테이블의 partner 참조 데이터 삭제
DROP TABLE IF EXISTS settlement CASCADE;

-- 3. product 테이블의 partner 참조 제거 (product는 partner가 nullable이므로 데이터는 유지)
-- product 테이블의 partner_id를 NULL로 설정
UPDATE product SET partner_id = NULL WHERE partner_id IS NOT NULL;

-- 4. partner 테이블 삭제
DROP TABLE IF EXISTS partner CASCADE;

-- 5. (선택) account 테이블에서 PARTNER 역할의 계정도 삭제하려면
-- DELETE FROM account WHERE role = 'PARTNER';

-- 완료 후 백엔드 애플리케이션을 재시작하면 테이블이 자동으로 재생성됩니다.
-- (spring.jpa.hibernate.ddl-auto=update 설정 시)
