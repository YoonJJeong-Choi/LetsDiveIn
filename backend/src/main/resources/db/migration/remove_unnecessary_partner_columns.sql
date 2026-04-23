-- PartnerEntity에서 불필요한 컬럼 제거
-- 제거 대상:
-- 1. password - AccountEntity에만 저장하는 것이 올바른 설계
-- 2. deactivation_approved_at - PartnerHistory에 중복 기록됨
-- 3. reactivation_approved_at - PartnerHistory에 중복 기록됨

-- password 컬럼 제거
ALTER TABLE partner DROP COLUMN IF EXISTS password;

-- deactivation_approved_at 컬럼 제거
ALTER TABLE partner DROP COLUMN IF EXISTS deactivation_approved_at;

-- reactivation_approved_at 컬럼 제거
ALTER TABLE partner DROP COLUMN IF EXISTS reactivation_approved_at;
