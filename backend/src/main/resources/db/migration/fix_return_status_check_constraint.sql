-- 반품 상태 체크 제약 조건 수정
-- PICKUP_COMPLETED 상태에서 REJECTED로 변경 가능하도록 수정

-- 기존 제약 조건 삭제
ALTER TABLE "return" DROP CONSTRAINT IF EXISTS return_return_status_check;

-- 새로운 제약 조건 추가 (모든 반품 상태 값 허용)
ALTER TABLE "return" ADD CONSTRAINT return_return_status_check 
    CHECK (return_status IN ('REQUESTED', 'APPROVED', 'REJECTED', 'PICKUP_COMPLETED', 'REFUNDED'));
