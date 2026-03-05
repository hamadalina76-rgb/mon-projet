-- Location Service - Add radius_km column to adm_zones

ALTER TABLE adm_zones
    ADD COLUMN IF NOT EXISTS radius_km INTEGER;

