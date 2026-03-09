-- ============================================
-- V10: Product stock (OneToOne with PRODUCTS)
-- Table PRODUCT_STOCK en MAJUSCULES, indexation
-- ============================================

CREATE TABLE IF NOT EXISTS PRODUCT_STOCK (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT    NOT NULL UNIQUE,
    quantity            INTEGER   NOT NULL DEFAULT 0,
    low_stock_threshold INTEGER   NOT NULL DEFAULT 0,
    is_tracking_enabled BOOLEAN   NOT NULL DEFAULT TRUE,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_stock_product FOREIGN KEY (product_id) REFERENCES PRODUCTS(id) ON DELETE CASCADE
);

-- Indexation PRODUCT_STOCK
CREATE INDEX IF NOT EXISTS idx_product_stock_product_id
    ON PRODUCT_STOCK (product_id);
CREATE INDEX IF NOT EXISTS idx_product_stock_tracking_quantity
    ON PRODUCT_STOCK (is_tracking_enabled, quantity);
CREATE INDEX IF NOT EXISTS idx_product_stock_low_stock
    ON PRODUCT_STOCK (is_tracking_enabled, quantity, low_stock_threshold) WHERE is_tracking_enabled = TRUE;
