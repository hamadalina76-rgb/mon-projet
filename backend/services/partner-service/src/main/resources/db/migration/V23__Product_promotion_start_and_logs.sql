-- Date de début de promotion sur les produits
ALTER TABLE products ADD COLUMN IF NOT EXISTS promotion_start_date DATE NULL;

-- Table des logs d'historique des promotions (chaque application de promo crée des entrées)
CREATE TABLE IF NOT EXISTS promotion_logs (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(255),
    promotion_label VARCHAR(100),
    promotion_start_date DATE,
    promotion_end_date DATE,
    discount_percentage DECIMAL(5,2),
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_logs_partner FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_logs_partner_applied ON promotion_logs(partner_id, applied_at DESC);
