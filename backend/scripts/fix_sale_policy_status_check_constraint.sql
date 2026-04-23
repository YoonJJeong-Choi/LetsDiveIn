-- sale_policy.status CHECK 제약조건을 최신 SaleStatus enum과 일치시킵니다.
-- 허용값: PENDING_APPROVAL, ACTIVE, INACTIVE, REJECTED, EXPIRED, CANCELLED

ALTER TABLE sale_policy
DROP CONSTRAINT IF EXISTS sale_policy_status_check;

ALTER TABLE sale_policy
ADD CONSTRAINT sale_policy_status_check
CHECK (
    status IN (
        'PENDING_APPROVAL',
        'ACTIVE',
        'INACTIVE',
        'REJECTED',
        'EXPIRED',
        'CANCELLED'
    )
);

