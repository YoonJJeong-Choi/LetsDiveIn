-- 파트너 이력 액션 타입 체크 제약 조건 수정
-- 신청 및 거절 액션 타입 추가

-- 기존 제약 조건 삭제
ALTER TABLE partner_history DROP CONSTRAINT IF EXISTS partner_history_action_type_check;

-- 새로운 제약 조건 추가 (모든 액션 타입 값 허용)
ALTER TABLE partner_history ADD CONSTRAINT partner_history_action_type_check 
    CHECK (action_type IN (
        'APPLICATION',
        'APPROVAL',
        'REJECTION',
        'DEACTIVATION_REQUEST',
        'DEACTIVATION_APPROVED',
        'DEACTIVATION_REJECTION',
        'REACTIVATION_REQUEST',
        'REACTIVATION_APPROVED',
        'REACTIVATION_REJECTION',
        'DEACTIVATED',
        'ACTIVATED'
    ));
