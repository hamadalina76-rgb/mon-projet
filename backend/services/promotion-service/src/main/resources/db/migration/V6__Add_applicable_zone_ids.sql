-- V6: Add applicable_zone_ids column for FREE_DELIVERY zone-based promotions
ALTER TABLE promotions ADD COLUMN IF NOT EXISTS applicable_zone_ids TEXT;
