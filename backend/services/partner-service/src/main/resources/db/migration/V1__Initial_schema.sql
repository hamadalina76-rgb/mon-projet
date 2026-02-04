-- ============================================
-- Partner Service - Initial Schema
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table: categories
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    icon VARCHAR(255),
    display_order INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: partners
CREATE TABLE IF NOT EXISTS partners (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT, -- Reference to auth-service users table (logical, no FK)
    business_name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) UNIQUE,
    type VARCHAR(50), -- RESTAURANT, STORE, CAFE
    description TEXT,
    logo VARCHAR(500),
    cover_image VARCHAR(500),
    location GEOGRAPHY(POINT, 4326),
    address JSONB, -- {street, city, postalCode, country}
    delivery_zone GEOGRAPHY(POLYGON, 4326),
    rating DECIMAL(3,2) DEFAULT 0,
    total_orders INTEGER DEFAULT 0,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACTIVE, SUSPENDED, INACTIVE
    opening_hours JSONB, -- {monday: {open: "09:00", close: "22:00"}, ...}
    preparation_time INTEGER DEFAULT 30, -- in minutes
    delivery_fee DECIMAL(10,2),
    minimum_order DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    accepts_orders BOOLEAN DEFAULT TRUE,
    phone VARCHAR(20),
    email VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: products
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    category_id BIGINT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    image VARCHAR(500),
    images JSONB, -- Array of image URLs
    is_available BOOLEAN DEFAULT TRUE,
    stock_quantity INTEGER,
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE, OUT_OF_STOCK
    preparation_time INTEGER, -- in minutes
    nutritional_info JSONB,
    allergens JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_partner FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE SET NULL
);

-- Table: product_options
CREATE TABLE IF NOT EXISTS product_options (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50), -- SIZE, FLAVOR, TOPPING, etc.
    is_required BOOLEAN DEFAULT FALSE,
    min_selection INTEGER DEFAULT 0,
    max_selection INTEGER DEFAULT 1,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_option_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Table: option_values
CREATE TABLE IF NOT EXISTS option_values (
    id BIGSERIAL PRIMARY KEY,
    option_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    price_modifier DECIMAL(10,2) DEFAULT 0,
    is_available BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_value_option FOREIGN KEY (option_id) REFERENCES product_options(id) ON DELETE CASCADE
);

-- Table: product_addons
CREATE TABLE IF NOT EXISTS product_addons (
    id BIGSERIAL PRIMARY KEY,
    product_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10,2) NOT NULL,
    is_available BOOLEAN DEFAULT TRUE,
    display_order INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_addon_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_partners_user_id ON partners(user_id);
CREATE INDEX IF NOT EXISTS idx_partners_slug ON partners(slug);
CREATE INDEX IF NOT EXISTS idx_partners_location ON partners USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_partners_status ON partners(status);
CREATE INDEX IF NOT EXISTS idx_partners_active ON partners(is_active, accepts_orders);
CREATE INDEX IF NOT EXISTS idx_products_partner ON products(partner_id);
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_products_status ON products(status);
CREATE INDEX IF NOT EXISTS idx_products_available ON products(is_available);
CREATE INDEX IF NOT EXISTS idx_product_options_product ON product_options(product_id);
CREATE INDEX IF NOT EXISTS idx_option_values_option ON option_values(option_id);
CREATE INDEX IF NOT EXISTS idx_product_addons_product ON product_addons(product_id);
