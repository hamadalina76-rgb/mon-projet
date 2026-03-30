-- ============================================================================
-- V24 : Table des paramètres globaux de la plateforme
-- ============================================================================

CREATE TABLE IF NOT EXISTS platform_settings (
    id              BIGSERIAL    PRIMARY KEY,
    setting_key     VARCHAR(100) NOT NULL UNIQUE,
    setting_value   TEXT         NOT NULL,
    description     VARCHAR(255),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed : valeurs par défaut
INSERT INTO platform_settings (setting_key, setting_value, description) VALUES
    ('PLATFORM_NAME',      'SpeedLine',  'Nom de la plateforme'),
    ('CONTACT_EMAIL',      '',           'E-mail de contact'),
    ('CONTACT_PHONE',      '',           'Téléphone de contact'),
    ('DEFAULT_LANGUAGE',   'fr',         'Langue par défaut'),
    ('DEFAULT_CURRENCY',   'MAD',        'Devise par défaut'),
    ('MAINTENANCE_MODE',   'false',      'Mode maintenance (true/false)'),
    ('APP_ENABLED',        'true',       'Application activée/désactivée (true/false)')
ON CONFLICT (setting_key) DO NOTHING;
