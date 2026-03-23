-- Add permission flag to allow partner to auto-approve product updates
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS allow_product_updates_without_approval BOOLEAN DEFAULT FALSE;

