-- ============================================
-- Location Service - Préfixe table ADM_ et noms en majuscules
-- ============================================
-- Renommage de la table zones -> ADM_ZONES et des index/trigger associés.

-- 1. Renommer la table
ALTER TABLE zones RENAME TO "ADM_ZONES";

-- 2. Renommer les index (noms en majuscules avec préfixe)
ALTER INDEX IF EXISTS idx_zones_boundary RENAME TO "IDX_ADM_ZONES_BOUNDARY";
ALTER INDEX IF EXISTS idx_zones_type RENAME TO "IDX_ADM_ZONES_TYPE";
ALTER INDEX IF EXISTS idx_zones_active RENAME TO "IDX_ADM_ZONES_ACTIVE";
ALTER INDEX IF EXISTS idx_zones_name RENAME TO "IDX_ADM_ZONES_NAME";
ALTER INDEX IF EXISTS idx_zones_city RENAME TO "IDX_ADM_ZONES_CITY";
ALTER INDEX IF EXISTS idx_zones_name_city RENAME TO "IDX_ADM_ZONES_NAME_CITY";

-- 3. Renommer le trigger (sync boundary depuis boundary_json)
ALTER TRIGGER trg_zones_sync_boundary ON "ADM_ZONES" RENAME TO "TRG_ADM_ZONES_SYNC_BOUNDARY";
