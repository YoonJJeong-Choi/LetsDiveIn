ALTER TABLE qna_draft_log
    ADD COLUMN IF NOT EXISTS admin_id BIGINT,
    ADD COLUMN IF NOT EXISTS partner_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_qna_draft_log_admin_created ON qna_draft_log(admin_id, created_at);
CREATE INDEX IF NOT EXISTS idx_qna_draft_log_partner_created ON qna_draft_log(partner_id, created_at);
CREATE INDEX IF NOT EXISTS idx_review_analysis_log_partner_created ON review_analysis_log(partner_id, created_at);
