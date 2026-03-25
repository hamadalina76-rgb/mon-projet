-- ============================================================
-- V21 : Table d'audit des modifications admin sur un livreur
-- ============================================================

CREATE TABLE courier_change_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    courier_id          BIGINT          NOT NULL,
    admin_id            BIGINT,
    admin_name          VARCHAR(150),
    action              VARCHAR(60)     NOT NULL,
    status_before       VARCHAR(50),
    status_after        VARCHAR(50),
    courier_type_before VARCHAR(50),
    courier_type_after  VARCHAR(50),
    zone_ids_before     VARCHAR(1000),
    zone_ids_after      VARCHAR(1000),
    reason              TEXT,
    changed_at          TIMESTAMP       NOT NULL
);

CREATE INDEX idx_ccl_courier_id ON courier_change_logs (courier_id, changed_at DESC);
