-- ============================================================
--  V28 : Table d'audit des paramètres globaux
--  Enregistre les modifications des horaires d'ouverture,
--  l'activation / désactivation de l'application.
-- ============================================================

CREATE TABLE IF NOT EXISTS settings_audit_logs (
    id          BIGSERIAL    PRIMARY KEY,
    action      VARCHAR(60)  NOT NULL,
    admin_id    BIGINT,
    admin_name  VARCHAR(100),
    details     TEXT,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sal_created_at ON settings_audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sal_action     ON settings_audit_logs (action);
