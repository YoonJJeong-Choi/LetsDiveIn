CREATE TABLE IF NOT EXISTS qna_draft_log (
    id              BIGSERIAL PRIMARY KEY,
    qna_no          BIGINT       NOT NULL,
    caller_role     VARCHAR(50),
    request_payload TEXT,
    response_payload TEXT,
    success         BOOLEAN      NOT NULL,
    error_message   VARCHAR(1000),
    latency_ms      BIGINT       NOT NULL,
    model           VARCHAR(50),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_qna_draft_log_qna_no ON qna_draft_log(qna_no);
