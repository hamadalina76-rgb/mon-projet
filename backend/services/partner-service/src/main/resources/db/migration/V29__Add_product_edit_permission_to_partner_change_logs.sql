-- Add product edit permission (allowProductUpdatesWithoutApproval) to partner change history
ALTER TABLE partner_change_logs
    ADD COLUMN IF NOT EXISTS product_edit_permission_before BOOLEAN,
    ADD COLUMN IF NOT EXISTS product_edit_permission_after  BOOLEAN;

