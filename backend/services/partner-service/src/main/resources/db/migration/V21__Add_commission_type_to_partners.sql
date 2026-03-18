-- ============================================
-- V21: Add commission_type column to partners table
-- ============================================

-- Add commission_type column to support PERCENTAGE and MARKUP commission types
ALTER TABLE partners 
ADD COLUMN commission_type VARCHAR(50) DEFAULT 'PERCENTAGE';

-- Add check constraint to ensure valid commission types
ALTER TABLE partners 
ADD CONSTRAINT chk_commission_type 
CHECK (commission_type IN ('PERCENTAGE', 'MARKUP'));

-- Update existing partners to use PERCENTAGE as default
UPDATE partners 
SET commission_type = 'PERCENTAGE' 
WHERE commission_type IS NULL;