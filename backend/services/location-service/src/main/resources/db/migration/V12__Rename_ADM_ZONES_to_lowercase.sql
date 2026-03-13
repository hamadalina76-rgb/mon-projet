-- Rename "ADM_ZONES" and its indexes to lowercase so Hibernate (default naming) finds them.
-- Idempotent: only run if the table is still named ADM_ZONES.

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ADM_ZONES') THEN
    ALTER TABLE "ADM_ZONES" RENAME TO adm_zones;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_BOUNDARY') THEN
    ALTER INDEX "IDX_ADM_ZONES_BOUNDARY" RENAME TO idx_adm_zones_boundary;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_TYPE') THEN
    ALTER INDEX "IDX_ADM_ZONES_TYPE" RENAME TO idx_adm_zones_type;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_ACTIVE') THEN
    ALTER INDEX "IDX_ADM_ZONES_ACTIVE" RENAME TO idx_adm_zones_active;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_NAME') THEN
    ALTER INDEX "IDX_ADM_ZONES_NAME" RENAME TO idx_adm_zones_name;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_CITY') THEN
    ALTER INDEX "IDX_ADM_ZONES_CITY" RENAME TO idx_adm_zones_city;
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_NAME_CITY') THEN
    ALTER INDEX "IDX_ADM_ZONES_NAME_CITY" RENAME TO idx_adm_zones_name_city;
  END IF;
END $$;

DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_trigger t
             JOIN pg_class c ON t.tgrelid = c.oid
             JOIN pg_namespace n ON c.relnamespace = n.oid
             WHERE n.nspname = 'public' AND c.relname = 'adm_zones' AND t.tgname = 'TRG_ADM_ZONES_SYNC_BOUNDARY') THEN
    ALTER TRIGGER "TRG_ADM_ZONES_SYNC_BOUNDARY" ON adm_zones RENAME TO trg_adm_zones_sync_boundary;
  END IF;
END $$;
