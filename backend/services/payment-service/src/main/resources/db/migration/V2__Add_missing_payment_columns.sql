-- ============================================
-- Payment Service - Add Missing Columns
-- ============================================

-- ==================== PAYMENTS TABLE ====================

-- Add missing columns to payments table
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS payment_method_id BIGINT,
    ADD COLUMN IF NOT EXISTS external_reference VARCHAR(100),
    ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS transaction_fee DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS error_code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_refund_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS client_ip VARCHAR(50),
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500),
    ADD COLUMN IF NOT EXISTS metadata_json TEXT;

-- Convert metadata JSONB to metadata_json TEXT if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payments' AND column_name = 'metadata' 
               AND data_type = 'jsonb') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'payments' AND column_name = 'metadata_json') THEN
        UPDATE payments 
        SET metadata_json = metadata::TEXT 
        WHERE metadata_json IS NULL AND metadata IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_method_id ON payments(payment_method_id);

-- ==================== PAYMENT_METHODS TABLE ====================

-- Rename token column to stripe_payment_method_id if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payment_methods' AND column_name = 'token') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'payment_methods' AND column_name = 'stripe_payment_method_id') THEN
        ALTER TABLE payment_methods RENAME COLUMN token TO stripe_payment_method_id;
    END IF;
END $$;

-- Add missing columns to payment_methods table
ALTER TABLE payment_methods
    ADD COLUMN IF NOT EXISTS expiry_month INTEGER,
    ADD COLUMN IF NOT EXISTS expiry_year INTEGER,
    ADD COLUMN IF NOT EXISTS cardholder_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_payment_method_id VARCHAR(500),
    ADD COLUMN IF NOT EXISTS fingerprint VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS billing_address_json TEXT,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;

-- Extract expiry month and year from expiry_date if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'payment_methods' AND column_name = 'expiry_date') THEN
        -- Extract month and year from MM/YYYY format
        UPDATE payment_methods 
        SET expiry_month = CAST(SUBSTRING(expiry_date, 1, 2) AS INTEGER),
            expiry_year = CAST(SUBSTRING(expiry_date, 4, 4) AS INTEGER)
        WHERE expiry_date IS NOT NULL 
          AND expiry_date ~ '^\d{2}/\d{4}$'
          AND (expiry_month IS NULL OR expiry_year IS NULL);
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_payment_methods_is_active ON payment_methods(is_active);
CREATE INDEX IF NOT EXISTS idx_payment_methods_is_verified ON payment_methods(is_verified);
CREATE INDEX IF NOT EXISTS idx_payment_methods_fingerprint ON payment_methods(fingerprint);

-- ==================== WALLETS TABLE ====================

-- Add missing columns to wallets table
ALTER TABLE wallets
    ADD COLUMN IF NOT EXISTS minimum_balance DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS daily_top_up_limit DECIMAL(10,2) DEFAULT 1000.00,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS last_transaction_at TIMESTAMP;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_wallets_is_active ON wallets(is_active);
CREATE INDEX IF NOT EXISTS idx_wallets_is_verified ON wallets(is_verified);

-- ==================== WALLET_TRANSACTIONS TABLE ====================

-- Add missing columns to wallet_transactions table
ALTER TABLE wallet_transactions
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'COMPLETED',
    ADD COLUMN IF NOT EXISTS metadata_json TEXT;

-- Copy data from timestamp to created_at if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'wallet_transactions' AND column_name = 'timestamp') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'wallet_transactions' AND column_name = 'created_at') THEN
        UPDATE wallet_transactions 
        SET created_at = timestamp 
        WHERE created_at IS NULL AND timestamp IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_reference_type ON wallet_transactions(reference_type);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_status ON wallet_transactions(status);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_created_at ON wallet_transactions(created_at DESC);
