-- V6: Add partner_id column to users table
-- This column stores the partner ID from partner-service for PARTNER role users

ALTER TABLE users 
ADD COLUMN partner_id BIGINT;

-- Add comment
COMMENT ON COLUMN users.partner_id IS 'Partner ID from partner-service (for PARTNER role users only)';

-- Create index for faster lookups
CREATE INDEX idx_users_partner_id ON users(partner_id) WHERE partner_id IS NOT NULL;
