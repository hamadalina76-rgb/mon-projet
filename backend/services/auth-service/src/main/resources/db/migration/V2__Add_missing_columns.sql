-- ============================================
-- Auth Service - Add Missing Columns
-- ============================================

-- ==================== USERS TABLE ====================

-- Add missing columns to users table
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS profile_picture VARCHAR(500),
    ADD COLUMN IF NOT EXISTS verification_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_password_token VARCHAR(255),
    ADD COLUMN IF NOT EXISTS reset_password_expires TIMESTAMP;

-- Add indexes for users
CREATE INDEX IF NOT EXISTS idx_users_verification_token ON users(verification_token);
CREATE INDEX IF NOT EXISTS idx_users_reset_password_token ON users(reset_password_token);

-- ==================== REFRESH_TOKENS TABLE ====================

-- Add missing columns to refresh_tokens table
ALTER TABLE refresh_tokens
    ADD COLUMN IF NOT EXISTS device_info VARCHAR(500),
    ADD COLUMN IF NOT EXISTS ip_address VARCHAR(50);

-- Add indexes for refresh_tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_device_info ON refresh_tokens(device_info);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_ip_address ON refresh_tokens(ip_address);
