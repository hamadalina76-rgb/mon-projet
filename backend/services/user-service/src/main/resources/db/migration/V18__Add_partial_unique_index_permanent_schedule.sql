-- V18: Enforce uniqueness at DB level for permanent schedules (week_start_date IS NULL).
-- The UNIQUE constraint on (courier_id, week_start_date) does NOT protect against
-- duplicates when week_start_date is NULL because NULL != NULL in PostgreSQL.
-- A partial unique index fills that gap.

CREATE UNIQUE INDEX IF NOT EXISTS uq_courier_permanent_schedule
    ON courier_schedules (courier_id)
    WHERE week_start_date IS NULL;
