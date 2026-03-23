CREATE TABLE IF NOT EXISTS product_history_backups (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    action VARCHAR(80) NOT NULL,
    actor_type VARCHAR(30),
    actor_id BIGINT,
    changes_before TEXT,
    changes_after TEXT,
    reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_phb_partner_created
    ON product_history_backups (partner_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_phb_product_created
    ON product_history_backups (product_id, created_at DESC);
