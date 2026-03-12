-- ================================================
-- Table audit_logs
-- ================================================
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id          BIGSERIAL PRIMARY KEY,
                                          admin_id    BIGINT        NOT NULL,
                                          action      VARCHAR(50)   NOT NULL,  -- CREATE | UPDATE | DELETE | DEACTIVATE | ACTIVATE
    entity_type VARCHAR(100)  NOT NULL,  -- CATEGORY
    entity_id   BIGINT        NOT NULL,
    reason      TEXT,
    timestamp   TIMESTAMP     NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_admin  ON audit_logs(admin_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ts     ON audit_logs(timestamp DESC);