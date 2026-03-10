-- ============================================
-- V9: Fix name_i18n Column
-- ============================================

-- Ajouter la colonne name_i18n si elle n'existe pas
ALTER TABLE categories ADD COLUMN IF NOT EXISTS name_i18n JSONB;

-- Remplir name_i18n à partir de name pour les lignes où c'est NULL ou vide
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
    END IF;
END
$$;