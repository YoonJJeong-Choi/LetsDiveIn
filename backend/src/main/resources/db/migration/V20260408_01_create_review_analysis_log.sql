CREATE TABLE IF NOT EXISTS review_analysis_log (
    id BIGSERIAL PRIMARY KEY,
    partner_id VARCHAR(100) NULL,
    from_at TIMESTAMP NULL,
    to_at TIMESTAMP NULL,
    product_nos_payload TEXT NULL,
    request_payload TEXT NULL,
    response_payload TEXT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000) NULL,
    latency_ms BIGINT NOT NULL,
    model VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_analysis_log_created_at
    ON review_analysis_log (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_review_analysis_log_partner_id
    ON review_analysis_log (partner_id);
