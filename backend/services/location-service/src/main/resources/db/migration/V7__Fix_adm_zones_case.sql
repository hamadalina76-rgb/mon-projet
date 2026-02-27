-- ============================================
-- Fix table name case: "ADM_ZONES" (quoted, case-sensitive) → adm_zones (lowercase)
-- This corrects V6 which accidentally created a case-sensitive uppercase table name.
-- Only renames if the quoted uppercase name still exists.
-- ============================================

DO $$
BEGIN
    -- Rename table if it exists with quoted uppercase name
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'ADM_ZONES'  -- exact case match in catalog
    ) THEN
        ALTER TABLE "ADM_ZONES" RENAME TO adm_zones;
    END IF;

    -- Rename indexes if they exist with uppercase quoted names
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_BOUNDARY') THEN
        ALTER INDEX "IDX_ADM_ZONES_BOUNDARY" RENAME TO idx_adm_zones_boundary;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_TYPE') THEN
        ALTER INDEX "IDX_ADM_ZONES_TYPE" RENAME TO idx_adm_zones_type;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_ACTIVE') THEN
        ALTER INDEX "IDX_ADM_ZONES_ACTIVE" RENAME TO idx_adm_zones_active;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_NAME') THEN
        ALTER INDEX "IDX_ADM_ZONES_NAME" RENAME TO idx_adm_zones_name;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_CITY') THEN
        ALTER INDEX "IDX_ADM_ZONES_CITY" RENAME TO idx_adm_zones_city;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'IDX_ADM_ZONES_NAME_CITY') THEN
        ALTER INDEX "IDX_ADM_ZONES_NAME_CITY" RENAME TO idx_adm_zones_name_city;
    END IF;
END
$$;
