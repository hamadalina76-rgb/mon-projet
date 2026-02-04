-- ============================================
-- User Service - Initial Schema
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table: customers
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- Reference to auth-service users table (logical, no FK)
    wallet_balance DECIMAL(10,2) DEFAULT 0,
    loyalty_points INTEGER DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    preferences JSONB,
    referral_code VARCHAR(50) UNIQUE,
    referred_by BIGINT, -- Reference to another customer
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: couriers
CREATE TABLE IF NOT EXISTS couriers (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL, -- Reference to auth-service users table (logical, no FK)
    vehicle_type VARCHAR(50), -- BIKE, MOTORCYCLE, CAR
    vehicle_number VARCHAR(50),
    vehicle_model VARCHAR(100),
    vehicle_color VARCHAR(50),
    driving_license VARCHAR(100),
    current_location GEOGRAPHY(POINT, 4326),
    status VARCHAR(50) DEFAULT 'IDLE', -- IDLE, BUSY, OFFLINE, SUSPENDED
    rating DECIMAL(3,2) DEFAULT 0,
    total_deliveries INTEGER DEFAULT 0,
    total_earnings DECIMAL(10,2) DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    is_online BOOLEAN DEFAULT FALSE,
    delivery_zone GEOGRAPHY(POLYGON, 4326),
    max_delivery_radius INTEGER, -- in meters
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: addresses
CREATE TABLE IF NOT EXISTS addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL, -- Reference to auth-service users table (logical, no FK)
    type VARCHAR(50), -- HOME, WORK, OTHER
    label VARCHAR(100),
    street VARCHAR(255),
    building VARCHAR(100),
    floor VARCHAR(50),
    apartment VARCHAR(50),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) DEFAULT 'Tunisia',
    location GEOGRAPHY(POINT, 4326),
    instructions TEXT,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);
CREATE INDEX IF NOT EXISTS idx_customers_referral_code ON customers(referral_code);
CREATE INDEX IF NOT EXISTS idx_couriers_user_id ON couriers(user_id);
CREATE INDEX IF NOT EXISTS idx_couriers_status ON couriers(status);
CREATE INDEX IF NOT EXISTS idx_couriers_location ON couriers USING GIST(current_location);
CREATE INDEX IF NOT EXISTS idx_couriers_available ON couriers(is_available, is_online);
CREATE INDEX IF NOT EXISTS idx_addresses_user_id ON addresses(user_id);
CREATE INDEX IF NOT EXISTS idx_addresses_location ON addresses USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_addresses_default ON addresses(user_id, is_default);
