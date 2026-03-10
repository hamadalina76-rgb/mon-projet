-- Promotion label and end date for products (badge display, e.g. "-20%", "Nouveau")
-- IF NOT EXISTS makes the migration idempotent (safe if columns were added manually or by a previous run)
ALTER TABLE products ADD COLUMN IF NOT EXISTS promotion_label VARCHAR(100) NULL;
ALTER TABLE products ADD COLUMN IF NOT EXISTS promotion_end_date DATE NULL;
