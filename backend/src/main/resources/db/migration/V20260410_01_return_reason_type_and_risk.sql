-- 반품 사유 유형 + 신청 시점 위험도 스냅샷
ALTER TABLE "return"
    ADD COLUMN IF NOT EXISTS return_reason_type VARCHAR(32);

ALTER TABLE "return"
    ADD COLUMN IF NOT EXISTS return_risk_score INTEGER;

ALTER TABLE "return"
    ADD COLUMN IF NOT EXISTS return_risk_tier VARCHAR(20);

-- 기존 행: 사유 유형 미기록 → 단순 변심으로 간주, 위험도는 보수적으로 LOW
UPDATE "return"
SET return_reason_type = 'CHANGE_OF_MIND'
WHERE return_reason_type IS NULL;

UPDATE "return"
SET return_risk_score = 0,
    return_risk_tier = 'LOW'
WHERE return_risk_score IS NULL OR return_risk_tier IS NULL;

ALTER TABLE "return"
    ALTER COLUMN return_reason_type SET NOT NULL;

ALTER TABLE "return"
    ALTER COLUMN return_risk_score SET NOT NULL;

ALTER TABLE "return"
    ALTER COLUMN return_risk_tier SET NOT NULL;
