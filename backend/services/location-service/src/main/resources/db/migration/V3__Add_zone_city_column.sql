-- ============================================
-- Location Service - Add City Column to Zones
-- ============================================

-- Add city column to zones table if it doesn't exist
ALTER TABLE zones
    ADD COLUMN IF NOT EXISTS city VARCHAR(100);

-- Create index on city column for faster queries
CREATE INDEX IF NOT EXISTS idx_zones_city ON zones(city);

-- Update existing zones with a default city if needed (optional)
-- UPDATE zones SET city = 'Tunis' WHERE city IS NULL;
