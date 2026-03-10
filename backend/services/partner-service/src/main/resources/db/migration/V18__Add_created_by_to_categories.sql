-- ============================================================
-- V18: Add created_by column to categories (required by Category.java entity)
--      and ensure audit_logs table exists (idempotent safety)
-- ============================================================

-- 1. categories.created_by (mapped by Category.java entity field createdBy)
ALTER TABLE categories ADD COLUMN IF NOT EXISTS created_by BIGINT;

-- 2. Ensure audit_logs table exists (V10 should have created it, but in case
--    of a partial migration history, re-create with IF NOT EXISTS)
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    admin_id    BIGINT       NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id   BIGINT       NOT NULL,
    reason      TEXT,
    timestamp   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin  ON audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ts     ON audit_logs(timestamp DESC);
