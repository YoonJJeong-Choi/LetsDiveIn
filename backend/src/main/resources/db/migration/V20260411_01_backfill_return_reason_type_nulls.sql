-- V20260410 미실행·부분 실패·수동 스키마 등으로 return_reason_type(및 위험도)에 NULL이 남은 경우 보정
-- NOT NULL 제약 적용 전에 반드시 실행되어야 함

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
