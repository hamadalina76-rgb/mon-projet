-- ============================================
-- V16: Fix categories schema after rebase/merge
-- Adds all columns expected by Category.java entity
-- that were missing from previous migrations
-- ============================================

-- 1. Rendre name nullable seulement si la colonne existe encore
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'name'
    ) THEN
        ALTER TABLE categories ALTER COLUMN name DROP NOT NULL;
    END IF;
END
$$;

-- 2. Ajouter les colonnes manquantes (idempotent IF NOT EXISTS)
ALTER TABLE categories ADD COLUMN IF NOT EXISTS slug            VARCHAR(255);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS image           VARCHAR(500);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS parent_id       BIGINT;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS is_featured     BOOLEAN      NOT NULL DEFAULT FALSE;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS category_type   VARCHAR(50)  DEFAULT 'PARTNER';
ALTER TABLE categories ADD COLUMN IF NOT EXISTS background_color VARCHAR(7);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS text_color       VARCHAR(7);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS partner_count    INTEGER      NOT NULL DEFAULT 0;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS product_count    INTEGER      NOT NULL DEFAULT 0;

-- 3. Générer des slugs à partir du nom existant pour les lignes sans slug
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'name'
    ) THEN
        UPDATE categories
        SET slug = LOWER(REGEXP_REPLACE(REGEXP_REPLACE(name, '[^a-zA-Z0-9\s-]', '', 'g'), '\s+', '-', 'g'))
        WHERE slug IS NULL AND name IS NOT NULL;
    END IF;
    -- Pour les lignes où name_i18n est rempli mais pas slug
    UPDATE categories
    SET slug = LOWER(REGEXP_REPLACE(
        REGEXP_REPLACE(
            COALESCE(name_i18n->>'fr', name_i18n->>'en', name_i18n->>'ar', 'category-' || id::text),
            '[^a-zA-Z0-9\s-]', '', 'g'
        ), '\s+', '-', 'g'
    ))
    WHERE slug IS NULL AND name_i18n IS NOT NULL AND name_i18n != '{}'::jsonb;
    -- Fallback pour toute ligne encore sans slug
    UPDATE categories SET slug = 'category-' || id::text WHERE slug IS NULL OR slug = '';
END
$$;

-- 4. Contrainte UNIQUE sur slug (après avoir peuplé toutes les lignes)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public' AND table_name = 'categories' AND constraint_name = 'uq_categories_slug'
    ) THEN
        ALTER TABLE categories ADD CONSTRAINT uq_categories_slug UNIQUE (slug);
    END IF;
END
$$;

-- 5. Index sur slug pour les lookups rapides
CREATE INDEX IF NOT EXISTS idx_categories_slug        ON categories (slug);
CREATE INDEX IF NOT EXISTS idx_categories_parent_id   ON categories (parent_id);
CREATE INDEX IF NOT EXISTS idx_categories_is_active   ON categories (is_active);
CREATE INDEX IF NOT EXISTS idx_categories_is_featured ON categories (is_featured);
