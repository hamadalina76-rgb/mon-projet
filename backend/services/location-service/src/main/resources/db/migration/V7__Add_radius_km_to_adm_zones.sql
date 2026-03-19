-- Location Service - Add radius_km column to ADM_ZONES (table name is case-sensitive)

ALTER TABLE "ADM_ZONES"
    ADD COLUMN IF NOT EXISTS radius_km INTEGER;

