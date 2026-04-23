CREATE TABLE IF NOT EXISTS return_risk_log (
    id BIGSERIAL PRIMARY KEY,
    product_no BIGINT NULL,
    option_no BIGINT NULL,
    request_payload TEXT NULL,
    response_payload TEXT NULL,
    score DOUBLE PRECISION NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000) NULL,
    latency_ms BIGINT NOT NULL,
    model VARCHAR(50) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_risk_log_created_at
    ON return_risk_log (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_return_risk_log_success
    ON return_risk_log (success);
