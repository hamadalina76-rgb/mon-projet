-- ============================================
-- Delivery Service - Bundle delivery columns
-- ============================================

ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS bundle_id BIGINT,
    ADD COLUMN IF NOT EXISTS promised_delivery_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS compensation_issued_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_deliveries_bundle_id ON deliveries(bundle_id);
