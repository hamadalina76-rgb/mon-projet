-- ============================================
-- Location Service - Préfixe table ADM_ et noms en majuscules
-- ============================================
-- Renommage de la table zones -> ADM_ZONES (idempotent : skip si déjà fait).

-- 1. Renommer la table seulement si ADM_ZONES n'existe pas encore
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'zones')
     AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'ADM_ZONES') THEN
    ALTER TABLE zones RENAME TO "ADM_ZONES";
  END IF;
END $$;

-- 2. Renommer les index seulement si l'ancien nom existe et le nouveau n'existe pas
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_boundary')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_BOUNDARY') THEN
    ALTER INDEX idx_zones_boundary RENAME TO "IDX_ADM_ZONES_BOUNDARY";
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_type')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_TYPE') THEN
    ALTER INDEX idx_zones_type RENAME TO "IDX_ADM_ZONES_TYPE";
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_active')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_ACTIVE') THEN
    ALTER INDEX idx_zones_active RENAME TO "IDX_ADM_ZONES_ACTIVE";
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_name')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_NAME') THEN
    ALTER INDEX idx_zones_name RENAME TO "IDX_ADM_ZONES_NAME";
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_city')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_CITY') THEN
    ALTER INDEX idx_zones_city RENAME TO "IDX_ADM_ZONES_CITY";
  END IF;
  IF EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'idx_zones_name_city')
     AND NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = 'public' AND indexname = 'IDX_ADM_ZONES_NAME_CITY') THEN
    ALTER INDEX idx_zones_name_city RENAME TO "IDX_ADM_ZONES_NAME_CITY";
  END IF;
END $$;

-- 3. Renommer le trigger seulement s'il existe encore sous l'ancien nom
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM pg_trigger t
             JOIN pg_class c ON t.tgrelid = c.oid
             JOIN pg_namespace n ON c.relnamespace = n.oid
             WHERE n.nspname = 'public' AND c.relname = 'ADM_ZONES' AND t.tgname = 'trg_zones_sync_boundary') THEN
    ALTER TRIGGER trg_zones_sync_boundary ON "ADM_ZONES" RENAME TO "TRG_ADM_ZONES_SYNC_BOUNDARY";
  END IF;
END $$;
