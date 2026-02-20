-- ============================================
-- Partner Service - Add Legal and Bank Information Columns
-- Migration V3 - Add missing columns for complete profile
-- ============================================

-- ==================== PARTNERS TABLE - BRAND AND DESCRIPTIONS ====================

-- Add brand name and short description columns
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS brand_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS short_description VARCHAR(500);

-- ==================== PARTNERS TABLE - LEGAL INFORMATION ====================

-- Add legal information columns
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS legal_status VARCHAR(100),
    ADD COLUMN IF NOT EXISTS tva VARCHAR(50),
    ADD COLUMN IF NOT EXISTS legal_rep_first_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS legal_rep_last_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS position VARCHAR(100);

-- Add indexes for legal information
CREATE INDEX IF NOT EXISTS idx_partners_tva ON partners(tva) WHERE tva IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_partners_legal_status ON partners(legal_status);

-- ==================== PARTNERS TABLE - BANK INFORMATION ====================

-- Add bank account columns
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS account_holder_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS iban VARCHAR(50),
    ADD COLUMN IF NOT EXISTS bank_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) DEFAULT 'TND';

-- Add indexes for bank information
CREATE INDEX IF NOT EXISTS idx_partners_iban ON partners(iban) WHERE iban IS NOT NULL;

-- ==================== PARTNERS TABLE - PAYMENT METHODS ====================

-- Add payment method columns
ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS accept_online_payment BOOLEAN DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS accept_cash_payment BOOLEAN DEFAULT TRUE;

-- Add indexes for payment methods
CREATE INDEX IF NOT EXISTS idx_partners_payment_methods ON partners(accept_online_payment, accept_cash_payment);

-- ==================== UPDATE EXISTING ROWS ====================

-- Set default values for existing rows
UPDATE partners 
SET 
    accept_online_payment = TRUE,
    accept_cash_payment = TRUE,
    currency = 'TND'
WHERE accept_online_payment IS NULL 
   OR accept_cash_payment IS NULL 
   OR currency IS NULL;

-- ==================== COMMENTS ====================

COMMENT ON COLUMN partners.brand_name IS 'Nom de marque commercial (peut être différent du nom légal)';
COMMENT ON COLUMN partners.short_description IS 'Description courte pour affichage dans les listes (max 500 caractères)';
COMMENT ON COLUMN partners.legal_status IS 'Statut juridique: SARL, SA, Auto-Entrepreneur, etc.';
COMMENT ON COLUMN partners.tva IS 'Numéro TVA intracommunautaire';
COMMENT ON COLUMN partners.legal_rep_first_name IS 'Prénom du représentant légal';
COMMENT ON COLUMN partners.legal_rep_last_name IS 'Nom du représentant légal';
COMMENT ON COLUMN partners.position IS 'Position/Fonction du représentant légal (Gérant, PDG, etc.)';
COMMENT ON COLUMN partners.account_holder_name IS 'Nom du titulaire du compte bancaire';
COMMENT ON COLUMN partners.iban IS 'IBAN du compte bancaire pour les virements';
COMMENT ON COLUMN partners.bank_name IS 'Nom de la banque';
COMMENT ON COLUMN partners.currency IS 'Devise du compte (TND, EUR, USD, etc.)';
COMMENT ON COLUMN partners.accept_online_payment IS 'Accepte les paiements en ligne (carte bancaire, wallet, etc.)';
COMMENT ON COLUMN partners.accept_cash_payment IS 'Accepte le paiement en espèces à la livraison';
