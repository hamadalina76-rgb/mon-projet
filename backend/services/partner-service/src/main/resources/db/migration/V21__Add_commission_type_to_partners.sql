-- ============================================
-- V21: Add commission_type column to partners table
-- ============================================

-- Add commission_type column to support PERCENTAGE and MARKUP commission types
ALTER TABLE partners
ADD COLUMN IF NOT EXISTS commission_type VARCHAR(50);

-- Ensure default is set even when the column was created manually beforehand
ALTER TABLE partners
ALTER COLUMN commission_type SET DEFAULT 'PERCENTAGE';

-- Add check constraint to ensure valid commission types
DO $$
BEGIN
	IF NOT EXISTS (
		SELECT 1
		FROM pg_constraint
		WHERE conname = 'chk_commission_type'
		  AND conrelid = 'partners'::regclass
	) THEN
		ALTER TABLE partners
		ADD CONSTRAINT chk_commission_type
		CHECK (commission_type IN ('PERCENTAGE', 'MARKUP'));
	END IF;
END $$;

-- Update existing partners to use PERCENTAGE as default
UPDATE partners
SET commission_type = 'PERCENTAGE'
WHERE commission_type IS NULL;