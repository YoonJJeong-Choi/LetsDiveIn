CREATE TABLE IF NOT EXISTS ai_blocked_log (
    id BIGSERIAL PRIMARY KEY,
    feature VARCHAR(40) NOT NULL,
    role VARCHAR(20) NOT NULL,
    account_id VARCHAR(100) NOT NULL,
    reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_blocked_log_created_at ON ai_blocked_log(created_at);
CREATE INDEX IF NOT EXISTS idx_ai_blocked_log_role_created_at ON ai_blocked_log(role, created_at);
