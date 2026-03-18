-- ============================================
-- Migration script for existing partners table
-- Execute this on existing databases that already have partners
-- ============================================

-- Add commission_type column
ALTER TABLE partners 
ADD COLUMN IF NOT EXISTS commission_type VARCHAR(50) DEFAULT 'PERCENTAGE';

-- Add check constraint to ensure valid commission types
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'chk_commission_type' AND table_name = 'partners'
    ) THEN
        ALTER TABLE partners 
        ADD CONSTRAINT chk_commission_type 
        CHECK (commission_type IN ('PERCENTAGE', 'MARKUP'));
    END IF;
END
$$;

-- Update existing partners to use PERCENTAGE as default
UPDATE partners 
SET commission_type = 'PERCENTAGE' 
WHERE commission_type IS NULL;

-- Verify the changes
SELECT COUNT(*) as total_partners, commission_type 
FROM partners 
GROUP BY commission_type;

COMMIT;