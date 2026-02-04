-- ============================================
-- Payment Service - Initial Schema
-- ============================================

-- Table: payments
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE NOT NULL, -- Reference to order-service (logical, no FK)
    transaction_id VARCHAR(255) UNIQUE, -- Stripe transaction ID
    method VARCHAR(50) NOT NULL, -- CARD, CASH, WALLET
    provider VARCHAR(50), -- STRIPE, CASH, WALLET
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(3) DEFAULT 'TND',
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    failure_reason TEXT,
    metadata JSONB -- Additional payment data
);

-- Table: payment_methods
CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- Reference to auth-service users (logical, no FK)
    type VARCHAR(50) NOT NULL, -- CARD, WALLET
    last4_digits VARCHAR(4),
    card_brand VARCHAR(50), -- VISA, MASTERCARD, etc.
    expiry_date VARCHAR(7), -- MM/YYYY format
    is_default BOOLEAN DEFAULT FALSE,
    token VARCHAR(500), -- Stripe payment method token
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: wallets
CREATE TABLE IF NOT EXISTS wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- Reference to auth-service users (logical, no FK)
    balance DECIMAL(10,2) DEFAULT 0,
    currency VARCHAR(3) DEFAULT 'TND',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: wallet_transactions
CREATE TABLE IF NOT EXISTS wallet_transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL, -- DEPOSIT, WITHDRAWAL, PAYMENT, REFUND, BONUS
    amount DECIMAL(10,2) NOT NULL,
    description TEXT,
    reference_id VARCHAR(255), -- Order ID, Payment ID, etc.
    balance_before DECIMAL(10,2),
    balance_after DECIMAL(10,2),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_transaction_wallet FOREIGN KEY (wallet_id) REFERENCES wallets(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_payments_order ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_transaction_id ON payments(transaction_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_created_at ON payments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_payment_methods_user ON payment_methods(user_id);
CREATE INDEX IF NOT EXISTS idx_payment_methods_default ON payment_methods(user_id, is_default);
CREATE INDEX IF NOT EXISTS idx_wallets_user ON wallets(user_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_wallet ON wallet_transactions(wallet_id);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_type ON wallet_transactions(type);
CREATE INDEX IF NOT EXISTS idx_wallet_transactions_timestamp ON wallet_transactions(timestamp DESC);
