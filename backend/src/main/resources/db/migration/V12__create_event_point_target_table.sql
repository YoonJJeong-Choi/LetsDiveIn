-- V12__create_event_point_target_table.sql
-- 포인트 이벤트 대상 매핑 테이블

CREATE TABLE IF NOT EXISTS event_point_target (
    id BIGSERIAL PRIMARY KEY,
    event_no BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_event_point_target_event
        FOREIGN KEY (event_no) REFERENCES event(event_no) ON DELETE CASCADE,
    CONSTRAINT uk_event_point_target_unique
        UNIQUE (event_no, target_type, target_value)
);

CREATE INDEX IF NOT EXISTS idx_event_point_target_event_type
    ON event_point_target (event_no, target_type);
