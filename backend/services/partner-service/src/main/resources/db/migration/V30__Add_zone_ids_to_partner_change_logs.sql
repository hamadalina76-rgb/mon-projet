-- Add zone IDs before/after to partner change history
ALTER TABLE partner_change_logs
    ADD COLUMN IF NOT EXISTS zone_ids_before VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS zone_ids_after  VARCHAR(1000);
