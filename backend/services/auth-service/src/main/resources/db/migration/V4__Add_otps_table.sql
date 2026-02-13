-- ============================================
-- Auth Service - Add OTP Table
-- ============================================

-- Table: otps
CREATE TABLE IF NOT EXISTS otps (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    is_used BOOLEAN NOT NULL DEFAULT FALSE,
    attempts_count INTEGER NOT NULL DEFAULT 0
);

-- Indexes for otps
CREATE INDEX IF NOT EXISTS idx_otps_email ON otps(email);
CREATE INDEX IF NOT EXISTS idx_otps_created_at ON otps(created_at);
CREATE INDEX IF NOT EXISTS idx_otps_expires_at ON otps(expires_at);
CREATE INDEX IF NOT EXISTS idx_otps_email_is_used ON otps(email, is_used);

-- Add comments
COMMENT ON TABLE otps IS 'Stores one-time passwords for two-factor authentication (login and forgot password)';
COMMENT ON COLUMN otps.email IS 'User email address';
COMMENT ON COLUMN otps.otp_code IS '6-digit OTP code';
COMMENT ON COLUMN otps.created_at IS 'Timestamp when OTP was created';
COMMENT ON COLUMN otps.expires_at IS 'Timestamp when OTP expires';
COMMENT ON COLUMN otps.is_used IS 'Whether OTP has been used';
COMMENT ON COLUMN otps.attempts_count IS 'Number of verification attempts';
