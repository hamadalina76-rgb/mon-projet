-- ============================================
-- MIGRATION: Migrate Categories to Multilingue
-- VERSION: 2
-- ============================================
-- Ajouter les colonnes manquantes et convertir name en nameI18n

BEGIN;

-- ============================================
-- 1️⃣ AJOUTER LES COLONNES MANQUANTES
-- ============================================

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS category_business_type VARCHAR(50) DEFAULT 'OTHER';

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS created_by BIGINT;

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- ============================================
-- 2️⃣ CRÉER LA COLONNE name_i18n JSONB
-- ============================================

ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS name_i18n JSONB;

-- ============================================
-- 3️⃣ MIGRER LES DONNÉES: name → name_i18n (si colonne name existe encore)
-- ============================================

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'categories' AND column_name = 'name'
    ) THEN
        UPDATE categories
        SET name_i18n = jsonb_build_object(
                'fr', COALESCE(name, ''),
                'ar', '',
                'en', COALESCE(name, '')
            )
        WHERE name_i18n IS NULL OR name_i18n = '{}'::jsonb;
    ELSE
        -- name déjà supprimée : mettre un objet vide pour les lignes sans name_i18n
        UPDATE categories
        SET name_i18n = '{}'::jsonb
        WHERE name_i18n IS NULL;
    END IF;
END
$$;

-- ============================================
-- 4️⃣ RENDRE name_i18n NOT NULL et UNIQUE
-- ============================================

ALTER TABLE categories
    ALTER COLUMN name_i18n SET NOT NULL;

-- ============================================
-- 5️⃣ CRÉER LES INDEXES MANQUANTS
-- ============================================

CREATE INDEX IF NOT EXISTS idx_category_business_type ON categories(category_business_type);
CREATE INDEX IF NOT EXISTS idx_category_created_at ON categories(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_categories_name_i18n ON categories USING GIN(name_i18n);

-- ============================================
-- 6️⃣ CRÉER LE TRIGGER POUR updated_at
-- ============================================

CREATE OR REPLACE FUNCTION update_categories_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_update_categories_updated_at ON categories;

CREATE TRIGGER trigger_update_categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW
    EXECUTE FUNCTION update_categories_updated_at();

-- ============================================
-- 7️⃣ SUPPRIMER L'ANCIENNE COLONNE name (optionnel)
-- ============================================

-- ALTER TABLE categories DROP COLUMN IF EXISTS name;
-- ⚠️ Commentée pour sécurité - décommenter après vérification

COMMIT;