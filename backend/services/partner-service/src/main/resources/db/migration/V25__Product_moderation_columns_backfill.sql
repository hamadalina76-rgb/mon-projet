-- ============================================
-- Partner Service - Product moderation backfill
-- ============================================
-- This migration is intentionally idempotent to handle cases where V24 was
-- recorded without actually applying the schema change.

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS moderation_status VARCHAR(50) DEFAULT 'APPROVED';

ALTER TABLE products
    ADD COLUMN IF NOT EXISTS moderation_reason TEXT;

UPDATE products
SET moderation_status = 'APPROVED'
WHERE moderation_status IS NULL;

CREATE INDEX IF NOT EXISTS idx_products_moderation_status
ON products(moderation_status);

