-- ============================================
-- V15: Ensure partner_menu_categories exists
-- Repair: V12 may have been marked applied without actually creating the table
-- ============================================

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'partner_menu_categories'
    ) THEN
        -- Drop any stale FKs first
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

        -- Rename old table if it exists, otherwise create from scratch
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = 'public' AND table_name = 'menu_categories'
        ) THEN
            ALTER TABLE menu_categories RENAME TO partner_menu_categories;
        ELSE
            CREATE TABLE partner_menu_categories (
                id          BIGSERIAL    PRIMARY KEY,
                partner_id  BIGINT       NOT NULL,
                name        VARCHAR(100) NOT NULL,
                description TEXT,
                image_url   VARCHAR(500),
                position    INTEGER      DEFAULT 0,
                is_visible  BOOLEAN      DEFAULT TRUE,
                created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
                updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
            );
        END IF;

        CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner
            ON partner_menu_categories (partner_id);
        CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner_position
            ON partner_menu_categories (partner_id, position);
    END IF;
    -- Table already exists: nothing to do
END
$$;
