ALTER TABLE courier_exceptional_schedules
    ADD COLUMN IF NOT EXISTS starts_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS ends_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_ces_starts_at ON courier_exceptional_schedules (starts_at);
CREATE INDEX IF NOT EXISTS idx_ces_ends_at ON courier_exceptional_schedules (ends_at);
