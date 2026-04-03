-- V4 — Create tables/columns that V3 couldn't apply (checksum mismatch)

-- Add status column to promotions
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS usage_limit_per_user INTEGER;

-- Table: promotion_rules
CREATE TABLE IF NOT EXISTS promotion_rules (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT       NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    rule_type    VARCHAR(50)  NOT NULL,
    operator     VARCHAR(10),
    target_value VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_promotion_rules_promo ON promotion_rules(promotion_id);

-- Table: promotion_usage_log (IF NOT EXISTS — may already exist from old V3)
CREATE TABLE IF NOT EXISTS promotion_usage_log (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT         NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    user_id         BIGINT         NOT NULL,
    order_id        BIGINT,
    discount_amount DECIMAL(10,2),
    status          VARCHAR(20)    NOT NULL DEFAULT 'APPLIED',
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_log_promotion ON promotion_usage_log(promotion_id);
CREATE INDEX IF NOT EXISTS idx_usage_log_user      ON promotion_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_log_order     ON promotion_usage_log(order_id);
