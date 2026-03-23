-- ============================================
-- Partner Service - Normalize legacy statuses
-- ============================================
-- Old deployments stored moderation values in products.status
-- (PENDING/APPROVED/REJECTED). The current domain model stores moderation
-- in moderation_status and keeps products.status for availability lifecycle.
--
-- This migration prevents runtime enum mapping failures like:
-- "No enum constant com.speedline.partner.domain.ProductStatus.PENDING"

-- 1) Move legacy moderation states from status -> moderation_status
UPDATE products
SET moderation_status = 'PENDING'
WHERE status = 'PENDING'
  AND (moderation_status IS NULL OR moderation_status = 'APPROVED');

UPDATE products
SET moderation_status = 'APPROVED'
WHERE status = 'APPROVED'
  AND (moderation_status IS NULL OR moderation_status = 'PENDING' OR moderation_status = 'REJECTED');

UPDATE products
SET moderation_status = 'REJECTED'
WHERE status = 'REJECTED'
  AND (moderation_status IS NULL OR moderation_status = 'APPROVED');

-- 2) Normalize products.status to valid ProductStatus enum values
UPDATE products
SET status = 'ACTIVE'
WHERE status IN ('PENDING', 'APPROVED', 'REJECTED');

