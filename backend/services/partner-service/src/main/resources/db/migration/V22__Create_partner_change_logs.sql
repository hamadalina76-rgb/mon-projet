-- ============================================
-- V22: Fix audit_logs missing columns + create partner_change_logs table
-- ============================================

-- 1. Fix existing audit_logs table (schema-validation error)
ALTER TABLE audit_logs
    ADD COLUMN IF NOT EXISTS changes_before TEXT,
    ADD COLUMN IF NOT EXISTS changes_after  TEXT;

-- 2. Dedicated table for partner modifications
CREATE TABLE IF NOT EXISTS partner_change_logs (
    id                      BIGSERIAL PRIMARY KEY,
    partner_id              BIGINT        NOT NULL,
    admin_id                BIGINT,
    action                  VARCHAR(60)   NOT NULL,   -- APPROVE, APPROVE_WITH_COMMISSION, REJECT, SUSPEND, ACTIVATE, DEACTIVATE

    -- Status
    status_before           VARCHAR(50),
    status_after            VARCHAR(50),

    -- Commission
    commission_type_before  VARCHAR(50),
    commission_type_after   VARCHAR(50),
    commission_rate_before  DECIMAL(10,2),
    commission_rate_after   DECIMAL(10,2),

    -- Categories (comma-separated IDs)
    category_ids_before     VARCHAR(500),
    category_ids_after      VARCHAR(500),

    -- Reason / note
    reason                  TEXT,

    changed_at              TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_partner_change_logs_partner_id
    ON partner_change_logs (partner_id, changed_at DESC);
