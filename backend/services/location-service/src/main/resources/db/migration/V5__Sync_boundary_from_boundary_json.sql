-- ============================================
-- Sync boundary (PostGIS) from boundary_json
-- ============================================
-- L'entité Zone ne mappe que boundary_json. La colonne boundary
-- (GEOGRAPHY NOT NULL) doit être remplie via trigger.

-- 1. Fonction trigger : remplit boundary depuis boundary_json
CREATE OR REPLACE FUNCTION sync_boundary_from_json()
RETURNS TRIGGER AS $$
BEGIN
  IF NEW.boundary_json IS NOT NULL 
     AND TRIM(NEW.boundary_json) != '' 
     AND TRIM(NEW.boundary_json) != '[]' 
     AND TRIM(NEW.boundary_json) LIKE '{%' THEN
    BEGIN
      NEW.boundary := ST_GeomFromGeoJSON(NEW.boundary_json)::geography;
    EXCEPTION WHEN OTHERS THEN
      RAISE EXCEPTION 'boundary_json invalide (GeoJSON Polygon requis): %', SQLERRM;
    END;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 2. Trigger BEFORE INSERT OR UPDATE
DROP TRIGGER IF EXISTS trg_zones_sync_boundary ON zones;
CREATE TRIGGER trg_zones_sync_boundary
  BEFORE INSERT OR UPDATE OF boundary_json ON zones
  FOR EACH ROW
  EXECUTE PROCEDURE sync_boundary_from_json();

-- 3. Corriger les lignes existantes où boundary est NULL
DO $$
DECLARE
  r RECORD;
BEGIN
  FOR r IN 
    SELECT id, boundary_json FROM zones 
    WHERE boundary IS NULL 
      AND boundary_json IS NOT NULL 
      AND TRIM(boundary_json) != '' 
      AND TRIM(boundary_json) != '[]' 
      AND TRIM(boundary_json) LIKE '{%'
  LOOP
    BEGIN
      UPDATE zones SET boundary = ST_GeomFromGeoJSON(r.boundary_json)::geography WHERE id = r.id;
    EXCEPTION WHEN OTHERS THEN
      RAISE NOTICE 'Zone % ignorée (boundary_json invalide): %', r.id, SQLERRM;
    END;
  END LOOP;
END $$;
