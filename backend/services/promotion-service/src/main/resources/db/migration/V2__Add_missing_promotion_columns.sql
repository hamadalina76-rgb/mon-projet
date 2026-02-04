-- ============================================
-- Promotion Service - Add Missing Columns
-- ============================================

-- ==================== PROMOTIONS TABLE ====================

-- Convert applicable_partners JSONB to applicable_partner_ids TEXT
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'promotions' AND column_name = 'applicable_partners' 
               AND data_type = 'jsonb') THEN
        -- Add new column
        ALTER TABLE promotions ADD COLUMN IF NOT EXISTS applicable_partner_ids TEXT;
        
        -- Copy data from JSONB to TEXT
        UPDATE promotions 
        SET applicable_partner_ids = applicable_partners::TEXT 
        WHERE applicable_partners IS NOT NULL AND applicable_partner_ids IS NULL;
        
        -- Drop old column
        ALTER TABLE promotions DROP COLUMN IF EXISTS applicable_partners;
    END IF;
END $$;

-- Convert applicable_categories JSONB to applicable_category_ids TEXT
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'promotions' AND column_name = 'applicable_categories' 
               AND data_type = 'jsonb') THEN
        -- Add new column
        ALTER TABLE promotions ADD COLUMN IF NOT EXISTS applicable_category_ids TEXT;
        
        -- Copy data from JSONB to TEXT
        UPDATE promotions 
        SET applicable_category_ids = applicable_categories::TEXT 
        WHERE applicable_categories IS NOT NULL AND applicable_category_ids IS NULL;
        
        -- Drop old column
        ALTER TABLE promotions DROP COLUMN IF EXISTS applicable_categories;
    END IF;
END $$;

-- Add missing columns
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS applicable_partner_ids TEXT,
    ADD COLUMN IF NOT EXISTS applicable_category_ids TEXT,
    ADD COLUMN IF NOT EXISTS first_order_only BOOLEAN DEFAULT FALSE;

-- Rename columns if needed (compatibility)
DO $$
BEGIN
    -- Rename user_usage_limit to usage_limit_per_user if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'promotions' AND column_name = 'user_usage_limit') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'promotions' AND column_name = 'usage_limit_per_user') THEN
        ALTER TABLE promotions RENAME COLUMN user_usage_limit TO usage_limit_per_user;
    END IF;
END $$;

-- Add missing column usage_limit_per_user if it doesn't exist
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS usage_limit_per_user INTEGER;

-- ==================== USER_PROMOTIONS TABLE ====================

-- Add missing columns to user_promotions table
ALTER TABLE user_promotions
    ADD COLUMN IF NOT EXISTS first_used_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS last_used_at TIMESTAMP;

-- Rename columns if needed (compatibility)
DO $$
BEGIN
    -- Rename first_used to first_used_at if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'user_promotions' AND column_name = 'first_used') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'user_promotions' AND column_name = 'first_used_at') THEN
        ALTER TABLE user_promotions RENAME COLUMN first_used TO first_used_at;
    END IF;
    
    -- Rename last_used to last_used_at if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'user_promotions' AND column_name = 'last_used') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'user_promotions' AND column_name = 'last_used_at') THEN
        ALTER TABLE user_promotions RENAME COLUMN last_used TO last_used_at;
    END IF;
END $$;
