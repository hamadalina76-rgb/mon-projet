ALTER TABLE courier_exceptional_schedules
    ADD COLUMN IF NOT EXISTS unavailability_reason VARCHAR(40),
    ADD COLUMN IF NOT EXISTS estimated_duration_minutes INTEGER,
    ADD COLUMN IF NOT EXISTS validation_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS validator_admin_id BIGINT,
    ADD COLUMN IF NOT EXISTS validator_admin_name VARCHAR(150),
    ADD COLUMN IF NOT EXISTS validation_comment TEXT,
    ADD COLUMN IF NOT EXISTS validated_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS courier_type VARCHAR(20);

CREATE INDEX IF NOT EXISTS idx_ces_validation_status ON courier_exceptional_schedules (validation_status);
CREATE INDEX IF NOT EXISTS idx_ces_courier_type ON courier_exceptional_schedules (courier_type);
CREATE INDEX IF NOT EXISTS idx_ces_reason ON courier_exceptional_schedules (unavailability_reason);
