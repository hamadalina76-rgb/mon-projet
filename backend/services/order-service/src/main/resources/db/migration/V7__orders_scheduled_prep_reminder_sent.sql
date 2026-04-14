-- Idempotence : un seul rappel préparation (15 min + temps de prépa) avant le créneau planifié
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS scheduled_prep_reminder_sent BOOLEAN NOT NULL DEFAULT FALSE;
