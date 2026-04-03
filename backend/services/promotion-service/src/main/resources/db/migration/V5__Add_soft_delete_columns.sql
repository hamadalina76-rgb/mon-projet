-- V5: Add soft delete columns to promotions
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_promotion_deleted ON promotions(deleted);
