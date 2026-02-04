-- ============================================
-- Promotion Service - Initial Schema
-- ============================================

-- Table: promotions
CREATE TABLE IF NOT EXISTS promotions (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(50) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT, FREE_DELIVERY
    value DECIMAL(10,2) NOT NULL, -- Percentage (0-100) or fixed amount
    discount_type VARCHAR(50), -- DISCOUNT, CASHBACK
    minimum_order DECIMAL(10,2) DEFAULT 0,
    maximum_discount DECIMAL(10,2), -- Maximum discount amount for percentage
    usage_limit INTEGER, -- Total usage limit (null = unlimited)
    usage_count INTEGER DEFAULT 0,
    user_usage_limit INTEGER DEFAULT 1, -- Per user usage limit
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE,
    applicable_partners JSONB, -- Array of partner IDs (null = all partners)
    applicable_categories JSONB, -- Array of category IDs (null = all categories)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: user_promotions
CREATE TABLE IF NOT EXISTS user_promotions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- Reference to auth-service users (logical, no FK)
    promotion_id BIGINT NOT NULL,
    usage_count INTEGER DEFAULT 0,
    first_used TIMESTAMP,
    last_used TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_promotion_promotion FOREIGN KEY (promotion_id) REFERENCES promotions(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_promotions_code ON promotions(code);
CREATE INDEX IF NOT EXISTS idx_promotions_active ON promotions(is_active, start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_promotions_dates ON promotions(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_user_promotions_user ON user_promotions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_promotions_promotion ON user_promotions(promotion_id);
CREATE INDEX IF NOT EXISTS idx_user_promotions_user_promotion ON user_promotions(user_id, promotion_id);
