-- V10__add_point_event_order_cap_tables.sql
-- 포인트 이벤트 지급 횟수 제한을 주문 기준으로 집계하기 위한 캡 테이블

CREATE TABLE IF NOT EXISTS admin_event_reward_order_cap (
    cap_no BIGSERIAL PRIMARY KEY,
    event_no BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_no BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_admin_event_reward_order_cap_event
        FOREIGN KEY (event_no) REFERENCES event(event_no) ON DELETE CASCADE,
    CONSTRAINT fk_admin_event_reward_order_cap_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE,
    CONSTRAINT uk_admin_event_reward_order_cap_event_customer_order
        UNIQUE (event_no, customer_id, order_no)
);

CREATE INDEX IF NOT EXISTS idx_admin_event_reward_order_cap_event_customer
    ON admin_event_reward_order_cap (event_no, customer_id);

CREATE TABLE IF NOT EXISTS partner_event_reward_order_cap (
    cap_no BIGSERIAL PRIMARY KEY,
    event_no BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    order_no BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_partner_event_reward_order_cap_event
        FOREIGN KEY (event_no) REFERENCES event(event_no) ON DELETE CASCADE,
    CONSTRAINT fk_partner_event_reward_order_cap_customer
        FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE,
    CONSTRAINT uk_partner_event_reward_order_cap_event_customer_order
        UNIQUE (event_no, customer_id, order_no)
);

CREATE INDEX IF NOT EXISTS idx_partner_event_reward_order_cap_event_customer
    ON partner_event_reward_order_cap (event_no, customer_id);
