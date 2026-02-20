-- ============================================
-- V11 - Fix missing columns in couriers table
-- Correction: identity_document_image et autres
-- colonnes manquantes détectées par Hibernate validation
-- ============================================

ALTER TABLE couriers
    ADD COLUMN IF NOT EXISTS driving_license_number   VARCHAR(100),
    ADD COLUMN IF NOT EXISTS driving_license_image    VARCHAR(500),
    ADD COLUMN IF NOT EXISTS driving_license_expiry   TIMESTAMP,
    ADD COLUMN IF NOT EXISTS identity_document_image  VARCHAR(500),
    ADD COLUMN IF NOT EXISTS profile_photo            VARCHAR(500),
    ADD COLUMN IF NOT EXISTS documents_verified       BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS current_latitude         DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS current_longitude        DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS last_location_update     TIMESTAMP,
    ADD COLUMN IF NOT EXISTS preferred_delivery_zone  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_delivery_id      BIGINT,
    ADD COLUMN IF NOT EXISTS total_ratings            INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS successful_deliveries    INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS cancelled_deliveries     INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS average_delivery_time    INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS total_distance_travelled DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS weekly_earnings          DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS available_balance        DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bank_iban                VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_account_holder      VARCHAR(100),
    ADD COLUMN IF NOT EXISTS last_login_at            TIMESTAMP;

-- Renommer driving_license → driving_license_number si l'ancienne colonne existe encore
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'driving_license'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers' AND column_name = 'driving_license_number'
    ) THEN
        ALTER TABLE couriers RENAME COLUMN driving_license TO driving_license_number;
    END IF;
END $$;

-- Mettre à jour la précision de total_earnings si nécessaire (DECIMAL(10,2) → DECIMAL(12,2))
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'couriers'
          AND column_name = 'total_earnings'
          AND numeric_precision < 12
    ) THEN
        ALTER TABLE couriers ALTER COLUMN total_earnings TYPE DECIMAL(12,2);
    END IF;
END $$;

-- Index utiles
CREATE INDEX IF NOT EXISTS idx_couriers_current_delivery    ON couriers(current_delivery_id);
CREATE INDEX IF NOT EXISTS idx_couriers_documents_verified  ON couriers(documents_verified);
