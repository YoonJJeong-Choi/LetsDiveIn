-- 반품 신청 시점 위험도 스냅샷 제거 (AI 보조는 별도 규칙·사기 평가 사용)
ALTER TABLE public.return DROP CONSTRAINT IF EXISTS return_return_risk_tier_check;
ALTER TABLE public.return DROP COLUMN IF EXISTS return_risk_score;
ALTER TABLE public.return DROP COLUMN IF EXISTS return_risk_tier;
