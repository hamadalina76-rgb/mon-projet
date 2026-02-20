-- ============================================
-- Partner Service - Add Document and Photo URL Columns
-- Migration V4 - Store uploaded file URLs in the database
-- ============================================

-- ==================== PARTNERS TABLE - DOCUMENT URLS ====================

ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS kbis_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS id_card_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS insurance_url VARCHAR(500),
    ADD COLUMN IF NOT EXISTS rib_url VARCHAR(500);

-- ==================== PARTNERS TABLE - PHOTOS ====================

ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS photos_json TEXT;

-- ==================== COMMENTS ====================

COMMENT ON COLUMN partners.kbis_url IS 'URL du document KBIS (extrait Kbis / registre du commerce)';
COMMENT ON COLUMN partners.id_card_url IS 'URL de la carte d''identité du représentant légal';
COMMENT ON COLUMN partners.insurance_url IS 'URL de l''attestation d''assurance';
COMMENT ON COLUMN partners.rib_url IS 'URL du RIB (Relevé d''Identité Bancaire)';
COMMENT ON COLUMN partners.photos_json IS 'URLs des photos du partenaire (JSON array)';
