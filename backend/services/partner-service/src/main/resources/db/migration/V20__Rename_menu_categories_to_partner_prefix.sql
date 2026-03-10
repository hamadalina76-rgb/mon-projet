-- ============================================
-- V12: Backward compatibility - rename menu_categories to partner_menu_categories
-- (meme nom de table que l entite MenuCategory / V8)
-- ============================================

DO $$
BEGIN
    -- Cas 1 : menu_categories existe -> renommer en partner_menu_categories
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'menu_categories')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'partner_menu_categories') THEN

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

        ALTER TABLE menu_categories RENAME TO partner_menu_categories;

        ALTER TABLE products
            ADD CONSTRAINT fk_product_partner_menu_category
            FOREIGN KEY (category_id) REFERENCES partner_menu_categories (id) ON DELETE SET NULL;

        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_menu_category_partner') THEN
            ALTER INDEX idx_menu_category_partner RENAME TO idx_partner_menu_category_partner;
        END IF;
        IF EXISTS (SELECT 1 FROM pg_indexes WHERE indexname = 'idx_menu_category_partner_position') THEN
            ALTER INDEX idx_menu_category_partner_position RENAME TO idx_partner_menu_category_partner_position;
        END IF;

    -- Cas 2 : aucune des deux tables n existe -> creer partner_menu_categories directement
    ELSIF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'partner_menu_categories') THEN
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
        CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner
            ON partner_menu_categories (partner_id);
        CREATE INDEX IF NOT EXISTS idx_partner_menu_category_partner_position
            ON partner_menu_categories (partner_id, position);
    END IF;

    -- Cas 3 : partner_menu_categories existe deja -> rien a faire
END
$$;
