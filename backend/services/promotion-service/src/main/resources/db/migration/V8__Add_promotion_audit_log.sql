-- V8: Add promotion_audit_log table for tracking all promotion changes

CREATE TABLE promotion_audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    promotion_id    BIGINT          NOT NULL,
    promotion_code  VARCHAR(50),
    action          VARCHAR(30)     NOT NULL,
    details         TEXT,
    performed_by    VARCHAR(255),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_promotion ON promotion_audit_log (promotion_id);
CREATE INDEX idx_audit_created   ON promotion_audit_log (created_at);
