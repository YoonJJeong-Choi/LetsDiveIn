-- 세일 이력 테이블 추가
CREATE TABLE IF NOT EXISTS sale_policy_history (
    history_id BIGSERIAL PRIMARY KEY,
    sale_policy_id BIGINT NOT NULL REFERENCES sale_policy(id),
    action VARCHAR(30) NOT NULL,
    changed_by_role VARCHAR(20) NOT NULL,
    changed_by_id BIGINT NOT NULL,
    reason VARCHAR(500),
    before_status VARCHAR(20),
    after_status VARCHAR(20),
    before_event_no BIGINT,
    after_event_no BIGINT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sale_policy_history_policy_id
    ON sale_policy_history(sale_policy_id);

CREATE INDEX IF NOT EXISTS idx_sale_policy_history_changed_at
    ON sale_policy_history(changed_at);

-- 주문 아이템 세일 이벤트 스냅샷 필드 추가
ALTER TABLE order_item
    ADD COLUMN IF NOT EXISTS applied_sale_event_no BIGINT,
    ADD COLUMN IF NOT EXISTS applied_sale_event_type VARCHAR(30);

