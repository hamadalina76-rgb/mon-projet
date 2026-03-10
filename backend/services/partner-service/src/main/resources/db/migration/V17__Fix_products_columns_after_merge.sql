-- ============================================
-- V17: Fix all missing columns after rebase/merge
-- V8 was modified by the merge; repair() updated its checksum
-- but did NOT re-run its SQL. This migration adds all columns
-- that V8 should have added, using IF NOT EXISTS everywhere.
-- ============================================

-- ==================== TABLE: products ====================

ALTER TABLE products ADD COLUMN IF NOT EXISTS short_description   VARCHAR(255);
ALTER TABLE products ADD COLUMN IF NOT EXISTS original_price      DECIMAL(10,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS discount_percentage DECIMAL(5,2);
ALTER TABLE products ADD COLUMN IF NOT EXISTS images_json         TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS nutritional_info_json TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS allergens_json      TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS ingredients_json    TEXT;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_vegetarian       BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_vegan            BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_halal            BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_gluten_free      BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS spicy_level         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_popular          BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_new              BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_featured         BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE products ADD COLUMN IF NOT EXISTS order_count         INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS rating              DECIMAL(3,2) NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS total_ratings       INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS display_order       INTEGER NOT NULL DEFAULT 0;
ALTER TABLE products ADD COLUMN IF NOT EXISTS tags                VARCHAR(500);

-- ==================== TABLE: product_options ====================

ALTER TABLE product_options ADD COLUMN IF NOT EXISTS description  VARCHAR(255);
ALTER TABLE product_options ADD COLUMN IF NOT EXISTS is_active    BOOLEAN NOT NULL DEFAULT TRUE;

-- Rename type column value from 'SIZE'/'FLAVOR' to enum names if needed
-- OptionType enum values (kept as-is, stored as STRING in Hibernate)
-- No action needed — column already exists as VARCHAR(50)

-- ==================== TABLE: option_values ====================

ALTER TABLE option_values ADD COLUMN IF NOT EXISTS description    VARCHAR(255);
ALTER TABLE option_values ADD COLUMN IF NOT EXISTS is_default     BOOLEAN NOT NULL DEFAULT FALSE;

-- ==================== TABLE: product_addons ====================

ALTER TABLE product_addons ADD COLUMN IF NOT EXISTS image         VARCHAR(500);
ALTER TABLE product_addons ADD COLUMN IF NOT EXISTS max_quantity  INTEGER;
ALTER TABLE product_addons ADD COLUMN IF NOT EXISTS category      VARCHAR(50);
ALTER TABLE product_addons ADD COLUMN IF NOT EXISTS is_popular    BOOLEAN NOT NULL DEFAULT FALSE;

-- ==================== FIX FK: products.category_id → partner_menu_categories ====================

DO $$
BEGIN
    -- Remove old FK pointing to categories (global) if it still exists
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_category' AND table_name = 'products'
    ) THEN
        ALTER TABLE products DROP CONSTRAINT fk_product_category;
    END IF;

    -- Add FK to partner_menu_categories if missing
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'partner_menu_categories'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_partner_menu_category' AND table_name = 'products'
    ) THEN
        -- Nullify orphan references first to avoid FK violation
        UPDATE products
        SET category_id = NULL
        WHERE category_id IS NOT NULL
          AND category_id NOT IN (SELECT id FROM partner_menu_categories);

        ALTER TABLE products
            ADD CONSTRAINT fk_product_partner_menu_category
            FOREIGN KEY (category_id) REFERENCES partner_menu_categories (id) ON DELETE SET NULL;
    END IF;
END
$$;

-- ==================== INDEXES ====================

CREATE INDEX IF NOT EXISTS idx_products_is_featured ON products(is_featured);
CREATE INDEX IF NOT EXISTS idx_products_is_popular  ON products(is_popular);
CREATE INDEX IF NOT EXISTS idx_products_display_order ON products(display_order);
CREATE INDEX IF NOT EXISTS idx_products_tags ON products(tags) WHERE tags IS NOT NULL;
