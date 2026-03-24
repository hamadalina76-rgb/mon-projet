-- V16: Replace break_duration (minutes integer) with break_start / break_end (TIME)
-- Applies to both schedule_template_shifts and courier_schedule_shifts
-- Idempotent: DROP IF EXISTS + ADD COLUMN IF NOT EXISTS

ALTER TABLE schedule_template_shifts
    DROP COLUMN IF EXISTS break_duration,
    ADD COLUMN IF NOT EXISTS break_start TIME,
    ADD COLUMN IF NOT EXISTS break_end   TIME;

ALTER TABLE courier_schedule_shifts
    DROP COLUMN IF EXISTS break_duration,
    ADD COLUMN IF NOT EXISTS break_start TIME,
    ADD COLUMN IF NOT EXISTS break_end   TIME;
