ALTER TABLE sale_policy
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(500);
