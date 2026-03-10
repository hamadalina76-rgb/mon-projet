-- ============================================
-- V11: Fix products.category_id FK to reference partner_menu_categories
-- (Certains schémas ont encore fk_product_menu_category → menu_categories)
-- ============================================

DO $$
BEGIN
    -- Supprimer l'ancienne FK si elle pointe vers menu_categories (table obsolète)
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_menu_category' AND table_name = 'products'
    ) THEN
        ALTER TABLE products DROP CONSTRAINT fk_product_menu_category;
    END IF;

    -- S'assurer que la FK vers partner_menu_categories existe
    -- (la table peut ne pas encore exister si V12 n'a pas encore renommé menu_categories)
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = 'public' AND table_name = 'partner_menu_categories'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_product_partner_menu_category' AND table_name = 'products'
    ) THEN
        ALTER TABLE products
            ADD CONSTRAINT fk_product_partner_menu_category
            FOREIGN KEY (category_id) REFERENCES partner_menu_categories (id) ON DELETE SET NULL;
    END IF;
END
$$;
