CREATE TABLE IF NOT EXISTS favorites (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    partner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorites_customer_partner UNIQUE (customer_id, partner_id),
    CONSTRAINT fk_favorites_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_favorites_customer_id ON favorites (customer_id);
CREATE INDEX IF NOT EXISTS idx_favorites_partner_id ON favorites (partner_id);
