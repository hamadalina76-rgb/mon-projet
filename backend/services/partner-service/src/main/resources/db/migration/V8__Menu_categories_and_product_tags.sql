-- ============================================
-- V8: Menu Categories table + product enhancements
-- Partner Service: table names UPPERCASE with prefix PARTNER_
-- ============================================

-- Create PARTNER_MENU_CATEGORIES table (per-partner menu categories)
-- PostgreSQL stores unquoted names as lowercase → partner_menu_categories
CREATE TABLE IF NOT EXISTS PARTNER_MENU_CATEGORIES (
    id            BIGSERIAL PRIMARY KEY,
    partner_id    BIGINT       NOT NULL,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    image_url     VARCHAR(500),
    position      INTEGER      DEFAULT 0,
    is_visible    BOOLEAN      DEFAULT TRUE,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner
    ON PARTNER_MENU_CATEGORIES (partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner_position
    ON PARTNER_MENU_CATEGORIES (partner_id, position);

-- ==================== PARTNER PRODUCTS: add missing columns ====================

ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS tags                VARCHAR(500);
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS short_description   VARCHAR(255);
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS original_price      DECIMAL(10,2);
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2);
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS images_json         TEXT;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS nutritional_info_json TEXT;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS allergens_json      TEXT;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS ingredients_json    TEXT;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_vegetarian       BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_vegan            BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_halal            BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_gluten_free      BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS spicy_level         INTEGER DEFAULT 0;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_popular          BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_new              BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS is_featured         BOOLEAN DEFAULT FALSE;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS order_count         INTEGER DEFAULT 0;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS rating              DECIMAL(3,2) DEFAULT 0;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS total_ratings       INTEGER DEFAULT 0;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS display_order       INTEGER DEFAULT 0;
ALTER TABLE PRODUCTS ADD COLUMN IF NOT EXISTS partner_id_col      BIGINT; -- No-op if already exists

-- ==================== PARTNER PRODUCT_OPTIONS: add missing columns ====================

ALTER TABLE PRODUCT_OPTIONS ADD COLUMN IF NOT EXISTS description  VARCHAR(255);
ALTER TABLE PRODUCT_OPTIONS ADD COLUMN IF NOT EXISTS is_active    BOOLEAN DEFAULT TRUE;

-- ==================== PARTNER OPTION_VALUES: add missing columns ====================

ALTER TABLE OPTION_VALUES ADD COLUMN IF NOT EXISTS description    VARCHAR(255);
ALTER TABLE OPTION_VALUES ADD COLUMN IF NOT EXISTS is_default     BOOLEAN DEFAULT FALSE;

-- ==================== PARTNER PRODUCT_ADDONS: add missing columns ====================

ALTER TABLE PRODUCT_ADDONS ADD COLUMN IF NOT EXISTS image         VARCHAR(500);
ALTER TABLE PRODUCT_ADDONS ADD COLUMN IF NOT EXISTS max_quantity  INTEGER;
ALTER TABLE PRODUCT_ADDONS ADD COLUMN IF NOT EXISTS category      VARCHAR(50);
ALTER TABLE PRODUCT_ADDONS ADD COLUMN IF NOT EXISTS is_popular    BOOLEAN DEFAULT FALSE;

-- ==================== Update FK: PRODUCTS.category_id → PARTNER_MENU_CATEGORIES ====================

DO $$
BEGIN
    -- Drop old FK pointing to global categories table if it exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_category'
          AND table_name      = 'products'
    ) THEN
        ALTER TABLE PRODUCTS DROP CONSTRAINT fk_product_category;
    END IF;

    -- Add new FK pointing to PARTNER_MENU_CATEGORIES if not already present
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_partner_menu_category'
          AND table_name      = 'products'
    ) THEN
        ALTER TABLE PRODUCTS
            ADD CONSTRAINT fk_product_partner_menu_category
                FOREIGN KEY (category_id) REFERENCES PARTNER_MENU_CATEGORIES (id) ON DELETE SET NULL;
    END IF;
END
$$;
