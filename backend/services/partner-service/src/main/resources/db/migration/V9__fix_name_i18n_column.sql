-- ============================================
-- MIGRATION: Fix name_i18n Column
-- VERSION: 3
-- ============================================

BEGIN;

-- Ajouter la colonne name_i18n si elle n'existe pas (nécessaire avant l'UPDATE)
ALTER TABLE categories ADD COLUMN IF NOT EXISTS name_i18n JSONB;

-- Remplir name_i18n à partir de name pour les lignes où c'est NULL ou vide
UPDATE categories
SET name_i18n = jsonb_build_object(
        'fr', COALESCE(name, ''),
        'ar', '',
        'en', COALESCE(name, '')
    )
WHERE name_i18n IS NULL OR name_i18n = '{}'::jsonb;

COMMIT;