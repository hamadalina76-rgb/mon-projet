-- ============================================
-- Partner Service - Add Missing Columns
-- ============================================

-- ==================== CATEGORIES TABLE ====================

-- Add missing columns to categories table
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS slug VARCHAR(100) UNIQUE,
    ADD COLUMN IF NOT EXISTS image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS parent_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS category_type VARCHAR(50) DEFAULT 'PARTNER',
    ADD COLUMN IF NOT EXISTS background_color VARCHAR(7),
    ADD COLUMN IF NOT EXISTS text_color VARCHAR(7),
    ADD COLUMN IF NOT EXISTS partner_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS product_count INTEGER DEFAULT 0;

-- Update icon column length if needed (from VARCHAR(255) to VARCHAR(500))
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'categories' 
               AND column_name = 'icon' 
               AND character_maximum_length < 500) THEN
        ALTER TABLE categories ALTER COLUMN icon TYPE VARCHAR(500);
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_categories_slug ON categories(slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id ON categories(parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_is_featured ON categories(is_featured);
CREATE INDEX IF NOT EXISTS idx_categories_category_type ON categories(category_type);

-- ==================== PARTNERS TABLE ====================

-- Handle address column conversion from JSONB to VARCHAR
DO $$
BEGIN
    -- Check if address column exists as JSONB
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'address' 
               AND data_type = 'jsonb') THEN
        -- Rename the JSONB column to address_jsonb
        ALTER TABLE partners RENAME COLUMN address TO address_jsonb;
    END IF;
END $$;

-- Add missing columns to partners table
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS address VARCHAR(255),
    ADD COLUMN IF NOT EXISTS city VARCHAR(100),
    ADD COLUMN IF NOT EXISTS postal_code VARCHAR(20),
    ADD COLUMN IF NOT EXISTS state VARCHAR(100),
    ADD COLUMN IF NOT EXISTS country VARCHAR(100) DEFAULT 'Tunisie',
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS delivery_radius INTEGER DEFAULT 5000,
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_premium BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS opening_hours_json TEXT,
    ADD COLUMN IF NOT EXISTS free_delivery_threshold DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS total_ratings INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_revenue DECIMAL(14,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS commission_rate DECIMAL(5,2) DEFAULT 15.00,
    ADD COLUMN IF NOT EXISTS category_ids VARCHAR(500),
    ADD COLUMN IF NOT EXISTS tags VARCHAR(500),
    ADD COLUMN IF NOT EXISTS phone_number VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename phone to phone_number if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'phone') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'partners' AND column_name = 'phone_number') THEN
        ALTER TABLE partners RENAME COLUMN phone TO phone_number;
    END IF;
    
    -- Rename user_id to userId if needed (should be user_id already)
    -- But check if userId exists
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'user_id') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'partners' AND column_name = 'user_id' AND is_nullable = 'YES') THEN
        -- Make user_id NOT NULL if needed
        UPDATE partners SET user_id = 0 WHERE user_id IS NULL;
        ALTER TABLE partners ALTER COLUMN user_id SET NOT NULL;
    END IF;
END $$;

-- Extract latitude and longitude from location GEOGRAPHY if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'location') THEN
        UPDATE partners 
        SET latitude = ST_Y(location::geometry)::DECIMAL(10,8),
            longitude = ST_X(location::geometry)::DECIMAL(11,8)
        WHERE (latitude IS NULL OR longitude IS NULL)
          AND location IS NOT NULL;
    END IF;
END $$;

-- Migrate data from address_jsonb (JSONB) to address (VARCHAR) and other address columns
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'address_jsonb' 
               AND data_type = 'jsonb') THEN
        -- Extract street from JSONB to address VARCHAR column
        UPDATE partners 
        SET address = address_jsonb->>'street'
        WHERE address_jsonb IS NOT NULL 
          AND address_jsonb->>'street' IS NOT NULL
          AND address IS NULL;
        
        -- Extract city from JSONB
        UPDATE partners 
        SET city = address_jsonb->>'city'
        WHERE address_jsonb IS NOT NULL 
          AND address_jsonb->>'city' IS NOT NULL
          AND city IS NULL;
        
        -- Extract postalCode from JSONB
        UPDATE partners 
        SET postal_code = address_jsonb->>'postalCode'
        WHERE address_jsonb IS NOT NULL 
          AND address_jsonb->>'postalCode' IS NOT NULL
          AND postal_code IS NULL;
        
        -- Extract country from JSONB
        UPDATE partners 
        SET country = COALESCE(address_jsonb->>'country', 'Tunisie')
        WHERE address_jsonb IS NOT NULL 
          AND country IS NULL;
    END IF;
END $$;

-- Convert opening_hours JSONB to opening_hours_json TEXT if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'partners' AND column_name = 'opening_hours' 
               AND data_type = 'jsonb') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'partners' AND column_name = 'opening_hours_json') THEN
        UPDATE partners 
        SET opening_hours_json = opening_hours::TEXT 
        WHERE opening_hours_json IS NULL AND opening_hours IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_partners_city ON partners(city);
CREATE INDEX IF NOT EXISTS idx_partners_state ON partners(state);
CREATE INDEX IF NOT EXISTS idx_partners_is_verified ON partners(is_verified);
CREATE INDEX IF NOT EXISTS idx_partners_is_premium ON partners(is_premium);
CREATE INDEX IF NOT EXISTS idx_partners_is_featured ON partners(is_featured);
CREATE INDEX IF NOT EXISTS idx_partners_total_ratings ON partners(total_ratings);

-- ==================== PRODUCTS TABLE ====================

-- Add missing columns to products table
ALTER TABLE products
    ADD COLUMN IF NOT EXISTS short_description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS original_price DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2),
    ADD COLUMN IF NOT EXISTS images_json TEXT,
    ADD COLUMN IF NOT EXISTS nutritional_info_json TEXT,
    ADD COLUMN IF NOT EXISTS allergens_json TEXT,
    ADD COLUMN IF NOT EXISTS ingredients_json TEXT,
    ADD COLUMN IF NOT EXISTS is_vegetarian BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_vegan BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_halal BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_gluten_free BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS spicy_level INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_popular BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_new BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS is_featured BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS order_count INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS rating DECIMAL(3,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_ratings INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS display_order INTEGER DEFAULT 0;

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename images JSONB to images_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'products' AND column_name = 'images' 
               AND data_type = 'jsonb') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'products' AND column_name = 'images_json') THEN
        UPDATE products 
        SET images_json = images::TEXT 
        WHERE images_json IS NULL AND images IS NOT NULL;
    END IF;
    
    -- Rename nutritional_info JSONB to nutritional_info_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'products' AND column_name = 'nutritional_info' 
               AND data_type = 'jsonb') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'products' AND column_name = 'nutritional_info_json') THEN
        UPDATE products 
        SET nutritional_info_json = nutritional_info::TEXT 
        WHERE nutritional_info_json IS NULL AND nutritional_info IS NOT NULL;
    END IF;
    
    -- Rename allergens JSONB to allergens_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'products' AND column_name = 'allergens' 
               AND data_type = 'jsonb') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'products' AND column_name = 'allergens_json') THEN
        UPDATE products 
        SET allergens_json = allergens::TEXT 
        WHERE allergens_json IS NULL AND allergens IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_products_is_featured ON products(is_featured);
CREATE INDEX IF NOT EXISTS idx_products_is_popular ON products(is_popular);
CREATE INDEX IF NOT EXISTS idx_products_is_new ON products(is_new);
CREATE INDEX IF NOT EXISTS idx_products_display_order ON products(display_order);
CREATE INDEX IF NOT EXISTS idx_products_rating ON products(rating);

-- ==================== PRODUCT_OPTIONS TABLE ====================

-- Add missing columns to product_options table
ALTER TABLE product_options
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Rename type column if needed (from VARCHAR(50) to match OptionType enum)
-- The type column already exists, we just need to ensure it's compatible

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_product_options_is_active ON product_options(is_active);

-- ==================== OPTION_VALUES TABLE ====================

-- Add missing columns to option_values table
ALTER TABLE option_values
    ADD COLUMN IF NOT EXISTS description VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_default BOOLEAN DEFAULT FALSE;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_option_values_is_default ON option_values(is_default);
CREATE INDEX IF NOT EXISTS idx_option_values_display_order ON option_values(display_order);

-- ==================== PRODUCT_ADDONS TABLE ====================

-- Add missing columns to product_addons table
ALTER TABLE product_addons
    ADD COLUMN IF NOT EXISTS image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS max_quantity INTEGER,
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS is_popular BOOLEAN DEFAULT FALSE;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_product_addons_is_popular ON product_addons(is_popular);
CREATE INDEX IF NOT EXISTS idx_product_addons_category ON product_addons(category);
CREATE INDEX IF NOT EXISTS idx_product_addons_display_order ON product_addons(display_order);
