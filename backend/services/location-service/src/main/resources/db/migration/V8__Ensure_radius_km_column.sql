-- V11: Ensure radius_km column exists in adm_zones.
-- V9 was recorded as SUCCESS in flyway_schema_history but the ALTER TABLE
-- never executed on this database instance. This migration re-applies it
-- idempotently (IF NOT EXISTS) so the column is guaranteed to be present.
ALTER TABLE "ADM_ZONES" ADD COLUMN IF NOT EXISTS radius_km INTEGER;
