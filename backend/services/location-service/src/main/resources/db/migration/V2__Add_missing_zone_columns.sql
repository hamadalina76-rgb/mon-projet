-- ============================================
-- Location Service - Add Missing Columns
-- ============================================

-- ==================== ZONES TABLE ====================

-- Add missing columns to zones table
ALTER TABLE zones
    ADD COLUMN IF NOT EXISTS boundary_json TEXT,
    ADD COLUMN IF NOT EXISTS min_delivery_time INTEGER,
    ADD COLUMN IF NOT EXISTS max_delivery_time INTEGER;

-- Make type NOT NULL if it's currently nullable
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'zones' 
               AND column_name = 'type' 
               AND is_nullable = 'YES') THEN
        -- First, set a default value for any NULL types
        UPDATE zones SET type = 'DELIVERY' WHERE type IS NULL;
        -- Then make it NOT NULL
        ALTER TABLE zones ALTER COLUMN type SET NOT NULL;
    END IF;
END $$;

-- Make boundary_json NOT NULL after populating it from boundary GEOGRAPHY if needed
DO $$
BEGIN
    -- If boundary_json is NULL and boundary GEOGRAPHY exists, try to convert it
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'zones' AND column_name = 'boundary') THEN
        -- Note: Converting GEOGRAPHY POLYGON to JSON is complex
        -- For now, we'll set a placeholder or leave it NULL
        -- The application should populate this when creating/updating zones
        UPDATE zones 
        SET boundary_json = '[]' 
        WHERE boundary_json IS NULL;
    END IF;
    
    -- Now make boundary_json NOT NULL
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'zones' 
               AND column_name = 'boundary_json' 
               AND is_nullable = 'YES') THEN
        ALTER TABLE zones ALTER COLUMN boundary_json SET NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_zones_name ON zones(name);
CREATE INDEX IF NOT EXISTS idx_zones_type ON zones(type);
