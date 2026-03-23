-- ============================================
-- Partner Service - Product moderation columns
-- ============================================

-- Add moderation status + reason to products table.
-- Existing rows must be treated as APPROVED to avoid hiding already-created products.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(50) DEFAULT 'APPROVED';

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

-- Ensure existing rows are backfilled when the default behavior doesn't backfill.
UPDATE products
SET moderation_status = 'APPROVED'
WHERE moderation_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_moderation_status
ON products(moderation_status);

