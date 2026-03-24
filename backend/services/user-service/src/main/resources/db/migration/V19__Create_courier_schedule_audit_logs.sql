-- V19: Table de traçabilité des modifications de plannings de livreurs
CREATE TABLE IF NOT EXISTS courier_schedule_audit_logs (
    id            BIGSERIAL     PRIMARY KEY,
    courier_id    BIGINT        NOT NULL,
    schedule_id   BIGINT,
    action        VARCHAR(50)   NOT NULL,   -- CREATED | UPDATED | TEMPLATE_APPLIED | DAY_COPIED | DELETED
    template_id   BIGINT,
    template_name VARCHAR(100),
    details       TEXT,
    admin_id      BIGINT,
    admin_name    VARCHAR(150),
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_csal_courier_created
    ON courier_schedule_audit_logs(courier_id, created_at DESC);
