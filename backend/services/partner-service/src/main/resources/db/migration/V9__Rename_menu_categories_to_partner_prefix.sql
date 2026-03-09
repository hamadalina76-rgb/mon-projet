-- ============================================
-- V9: Backward compatibility — rename menu_categories to partner_menu_categories
-- (même nom de table que l’entité MenuCategory / V8)
-- ============================================

DO $$
BEGIN
    -- If menu_categories exists and partner_menu_categories does not, rename
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'menu_categories')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'partner_menu_categories') THEN
        -- Drop FK from products if it points to menu_categories
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_product_menu_category' AND table_name = 'products'
        ) THEN
            ALTER TABLE products DROP CONSTRAINT fk_product_menu_category;
        END IF;
        IF EXISTS (
            SELECT 1 FROM information_schema.table_constraints
            WHERE constraint_name = 'fk_product_partner_menu_category' AND table_name = 'products'
        ) THEN
            ALTER TABLE products DROP CONSTRAINT fk_product_partner_menu_category;
        END IF;
        -- Rename table and indexes
        ALTER TABLE menu_categories RENAME TO partner_menu_categories;
        -- Recreate FK
        ALTER TABLE products
            ADD CONSTRAINT fk_product_partner_menu_category
            FOREIGN KEY (category_id) REFERENCES partner_menu_categories (id) ON DELETE SET NULL;
        -- Rename indexes if they exist (optional; PostgreSQL may have kept old names)
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_menu_category_partner') THEN
            ALTER INDEX idx_menu_category_partner RENAME TO idx_partner_menu_category_partner;
        END IF;
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_menu_category_partner_position') THEN
            ALTER INDEX idx_menu_category_partner_position RENAME TO idx_partner_menu_category_partner_position;
        END IF;
    END IF;
END
$$;
