-- ============================================
-- Location Service - Fix zones for GeoJSON format and city
-- ============================================
-- Corrige le format boundary_json pour PostGIS (GeoJSON Polygon)
-- et assure que city a une valeur par défaut

-- 1. Mettre à jour les zones avec city NULL
UPDATE zones SET city = 'Tunis' WHERE city IS NULL;

-- 2. Index composite pour findByNameAndCity
CREATE INDEX IF NOT EXISTS idx_zones_name_city ON zones(name, city);

-- 3. Si la colonne boundary (GEOGRAPHY) existe, copier vers boundary_json en GeoJSON
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.columns 
             WHERE table_name = 'zones' AND column_name = 'boundary') THEN
    UPDATE zones z
    SET boundary_json = ST_AsGeoJSON(z.boundary)
    WHERE (boundary_json IS NULL OR boundary_json = '[]' OR TRIM(boundary_json) = '[]')
      AND z.boundary IS NOT NULL;
  END IF;
EXCEPTION WHEN OTHERS THEN
  RAISE NOTICE 'Conversion boundary->boundary_json ignorée: %', SQLERRM;
END $$;

-- 4. Convertir boundary_json de [[lat,lon],...] vers GeoJSON Polygon
--    PostGIS ST_GeomFromGeoJSON attend: {"type":"Polygon","coordinates":[[[lon,lat],...]]}
DO $$
DECLARE
  r RECORD;
  coords jsonb;
  ring jsonb;
  ring_with_closure jsonb;
  converted jsonb;
  first_pt jsonb;
BEGIN
  FOR r IN 
    SELECT id, boundary_json 
    FROM zones 
    WHERE boundary_json IS NOT NULL 
      AND TRIM(boundary_json) != '[]'
      AND TRIM(boundary_json) NOT LIKE '{"type"%'
  LOOP
    BEGIN
      coords := r.boundary_json::jsonb;
      IF jsonb_typeof(coords) = 'array' AND jsonb_array_length(coords) >= 3 THEN
        -- Convertir [lat,lon] -> [lon,lat] pour chaque point
        SELECT jsonb_agg(
          jsonb_build_array((elem->>1)::float, (elem->>0)::float)
          ORDER BY ord
        )
        INTO ring
        FROM jsonb_array_elements(coords) WITH ORDINALITY AS t(elem, ord);
        
        -- Fermer le ring: ajouter le premier point à la fin
        first_pt := ring->0;
        ring_with_closure := ring || jsonb_build_array(first_pt);
        
        converted := jsonb_build_object(
          'type', 'Polygon',
          'coordinates', jsonb_build_array(ring_with_closure)
        );
        
        UPDATE zones SET boundary_json = converted::text WHERE id = r.id;
      END IF;
    EXCEPTION WHEN OTHERS THEN
      RAISE NOTICE 'Zone % ignorée (boundary_json invalide): %', r.id, SQLERRM;
    END;
  END LOOP;
END $$;
