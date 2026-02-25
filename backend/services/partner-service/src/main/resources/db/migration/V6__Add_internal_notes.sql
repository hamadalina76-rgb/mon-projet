-- ============================================
-- Partner Service - Add Internal Notes Column
-- Migration V6 - Notes internes (admin/support uniquement)
-- ============================================

ALTER TABLE partners
    ADD COLUMN IF NOT EXISTS internal_notes VARCHAR(1000);

COMMENT ON COLUMN partners.internal_notes IS 'Notes internes pour admin/support (non visibles par le partenaire)';
