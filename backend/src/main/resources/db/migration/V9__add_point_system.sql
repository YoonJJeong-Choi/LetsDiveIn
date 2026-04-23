-- V9__add_point_system.sql
-- 포인트 시스템 추가

-- customer 테이블에 point_balance 컬럼 추가
ALTER TABLE customer ADD COLUMN IF NOT EXISTS point_balance BIGINT NOT NULL DEFAULT 0;

-- 기존 고객의 point_balance를 0으로 초기화 (이미 0이지만 명시적으로 설정)
UPDATE customer SET point_balance = 0 WHERE point_balance IS NULL;

-- point_history 테이블 생성
CREATE TABLE IF NOT EXISTS point_history (
    history_id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    point_type VARCHAR(50) NOT NULL CHECK (point_type IN ('ACCUMULATE', 'USE', 'MANUAL_ADD', 'MANUAL_DEDUCT', 'EXPIRE')),
    point_amount BIGINT NOT NULL,
    point_balance_after BIGINT NOT NULL,
    order_item_no BIGINT,
    order_no BIGINT,
    description VARCHAR(500),
    expire_date DATE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    admin_id BIGINT,
    CONSTRAINT fk_point_history_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE,
    CONSTRAINT fk_point_history_admin FOREIGN KEY (admin_id) REFERENCES admin(admin_id) ON DELETE SET NULL
);

-- 인덱스 생성 (조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_point_history_customer_id ON point_history(customer_id);
CREATE INDEX IF NOT EXISTS idx_point_history_created_at ON point_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_point_history_point_type ON point_history(point_type);
