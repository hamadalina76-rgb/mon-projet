-- ============================================
-- User Service - Add Missing Columns
-- ============================================

-- ==================== ADDRESSES TABLE ====================

-- Add missing columns to addresses table
ALTER TABLE addresses 
    ADD COLUMN IF NOT EXISTS customer_id BIGINT,
    ADD COLUMN IF NOT EXISTS access_code VARCHAR(50),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS formatted_address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS place_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS delivery_instructions VARCHAR(500),
    ADD COLUMN IF NOT EXISTS landmark VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS usage_count INTEGER DEFAULT 0;

-- Rename 'instructions' to 'delivery_instructions' if it exists and delivery_instructions is null
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'addresses' AND column_name = 'instructions') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'addresses' AND column_name = 'delivery_instructions') THEN
        ALTER TABLE addresses RENAME COLUMN instructions TO delivery_instructions;
    END IF;
END $$;

-- Update existing records: set customer_id = user_id if customer_id is null
UPDATE addresses SET customer_id = user_id WHERE customer_id IS NULL;

-- Add indexes for addresses
CREATE INDEX IF NOT EXISTS idx_addresses_customer_id ON addresses(customer_id);
CREATE INDEX IF NOT EXISTS idx_addresses_is_active ON addresses(is_active);

-- ==================== COURIERS TABLE ====================

-- Add missing columns to couriers table
ALTER TABLE couriers
    ADD COLUMN IF NOT EXISTS driving_license_number VARCHAR(100),
    ADD COLUMN IF NOT EXISTS driving_license_image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS driving_license_expiry TIMESTAMP,
    ADD COLUMN IF NOT EXISTS identity_document_image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_photo VARCHAR(500),
    ADD COLUMN IF NOT EXISTS documents_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS current_latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS current_longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS last_location_update TIMESTAMP,
    ADD COLUMN IF NOT EXISTS preferred_delivery_zone VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_delivery_id BIGINT,
    ADD COLUMN IF NOT EXISTS total_ratings INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS successful_deliveries INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_deliveries INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS average_delivery_time INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_distance_travelled DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS weekly_earnings DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_balance DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bank_iban VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_account_holder VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename driving_license to driving_license_number if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'couriers' AND column_name = 'driving_license') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'couriers' AND column_name = 'driving_license_number') THEN
        ALTER TABLE couriers RENAME COLUMN driving_license TO driving_license_number;
    END IF;
END $$;

-- Update total_earnings precision if needed (from DECIMAL(10,2) to DECIMAL(12,2))
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'couriers' 
               AND column_name = 'total_earnings' 
               AND numeric_precision < 12) THEN
        ALTER TABLE couriers ALTER COLUMN total_earnings TYPE DECIMAL(12,2);
    END IF;
END $$;

-- Add indexes for couriers
CREATE INDEX IF NOT EXISTS idx_couriers_current_delivery ON couriers(current_delivery_id);
CREATE INDEX IF NOT EXISTS idx_couriers_documents_verified ON couriers(documents_verified);

-- ==================== CUSTOMERS TABLE ====================

-- Remove the JSONB preferences column if it exists (replaced by individual columns)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'customers' AND column_name = 'preferences') THEN
        ALTER TABLE customers DROP COLUMN preferences;
    END IF;
END $$;

-- Add missing columns to customers table
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS total_spent DECIMAL(12,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS average_rating_given DECIMAL(3,2),
    ADD COLUMN IF NOT EXISTS last_order_date TIMESTAMP,
    ADD COLUMN IF NOT EXISTS favorite_partner_ids VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS favorite_product_ids VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS status VARCHAR(50) DEFAULT 'ACTIVE',
    ADD COLUMN IF NOT EXISTS is_vip BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS vip_level VARCHAR(20),
    ADD COLUMN IF NOT EXISTS referred_by_customer_id BIGINT,
    ADD COLUMN IF NOT EXISTS successful_referrals INTEGER DEFAULT 0;

-- Add CustomerPreferences embedded columns
ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS language VARCHAR(10),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10),
    ADD COLUMN IF NOT EXISTS push_notifications_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS email_notifications_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS sms_notifications_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS marketing_emails_enabled BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS dark_mode_enabled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS default_search_radius INTEGER DEFAULT 5000,
    ADD COLUMN IF NOT EXISTS favorite_categories VARCHAR(500),
    ADD COLUMN IF NOT EXISTS dietary_restrictions VARCHAR(500),
    ADD COLUMN IF NOT EXISTS allergens VARCHAR(500);

-- Rename 'referred_by' to 'referred_by_customer_id' if needed (compatibilité)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'customers' AND column_name = 'referred_by') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                       WHERE table_name = 'customers' AND column_name = 'referred_by_customer_id') THEN
        ALTER TABLE customers RENAME COLUMN referred_by TO referred_by_customer_id;
    END IF;
END $$;

-- Add indexes for customers
CREATE INDEX IF NOT EXISTS idx_customers_status ON customers(status);
CREATE INDEX IF NOT EXISTS idx_customers_is_vip ON customers(is_vip);
CREATE INDEX IF NOT EXISTS idx_customers_referred_by ON customers(referred_by_customer_id);
