-- ============================================
-- Promotion Service V3 — Rules, Usage Log, Status
-- ============================================

-- Add status column to promotions
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';

-- usage_limit_per_user alias (V2 may have added user_usage_limit under a different name)
ALTER TABLE promotions
    ADD COLUMN IF NOT EXISTS usage_limit_per_user INTEGER;

-- Sync user_usage_limit → usage_limit_per_user if old column exists
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'promotions' AND column_name = 'user_usage_limit'
    ) THEN
        UPDATE promotions SET usage_limit_per_user = user_usage_limit WHERE usage_limit_per_user IS NULL;
    END IF;
END $$;

-- Table: promotion_rules
CREATE TABLE IF NOT EXISTS promotion_rules (
    id           BIGSERIAL PRIMARY KEY,
    promotion_id BIGINT       NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    rule_type    VARCHAR(50)  NOT NULL,
    operator     VARCHAR(10),
    target_value VARCHAR(255),
    CONSTRAINT fk_rule_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_promotion_rules_promo ON promotion_rules(promotion_id);

-- Table: promotion_usage_log
CREATE TABLE IF NOT EXISTS promotion_usage_log (
    id             BIGSERIAL PRIMARY KEY,
    promotion_id   BIGINT         NOT NULL REFERENCES promotions(id) ON DELETE CASCADE,
    user_id        BIGINT         NOT NULL,
    order_id       BIGINT,
    discount_amount DECIMAL(10,2),
    status         VARCHAR(20)    NOT NULL DEFAULT 'APPLIED',
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_log_promotion ON promotion_usage_log(promotion_id);
CREATE INDEX IF NOT EXISTS idx_usage_log_user      ON promotion_usage_log(user_id);
CREATE INDEX IF NOT EXISTS idx_usage_log_order     ON promotion_usage_log(order_id);
