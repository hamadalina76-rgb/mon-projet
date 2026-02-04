-- ============================================
-- Delivery Service - Initial Schema
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table: deliveries
CREATE TABLE IF NOT EXISTS deliveries (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT UNIQUE NOT NULL, -- Reference to order-service (logical, no FK)
    courier_id BIGINT NOT NULL, -- Reference to user-service courier (logical, no FK)
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING, ASSIGNED, ACCEPTED, PICKED_UP, IN_DELIVERY, DELIVERED, CANCELLED
    pickup_location GEOGRAPHY(POINT, 4326) NOT NULL,
    dropoff_location GEOGRAPHY(POINT, 4326) NOT NULL,
    pickup_address TEXT,
    dropoff_address TEXT,
    pickup_time TIMESTAMP,
    dropoff_time TIMESTAMP,
    distance DECIMAL(10,2), -- in kilometers
    estimated_duration INTEGER, -- in minutes
    actual_duration INTEGER, -- in minutes
    proof_of_delivery VARCHAR(500), -- URL to image/signature
    delivery_instructions TEXT,
    customer_name VARCHAR(255), -- Denormalized
    customer_phone VARCHAR(20), -- Denormalized
    partner_name VARCHAR(255), -- Denormalized
    partner_address TEXT, -- Denormalized
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table: tracking_points
CREATE TABLE IF NOT EXISTS tracking_points (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    speed DECIMAL(5,2), -- in km/h
    bearing DECIMAL(5,2), -- in degrees (0-360)
    accuracy DECIMAL(5,2), -- in meters
    CONSTRAINT fk_tracking_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
);

-- Table: routes (for route optimization)
CREATE TABLE IF NOT EXISTS routes (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    route_data JSONB, -- Route geometry and waypoints from Mapbox
    distance DECIMAL(10,2), -- in kilometers
    duration INTEGER, -- in seconds
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_route_delivery FOREIGN KEY (delivery_id) REFERENCES deliveries(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_deliveries_order ON deliveries(order_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_courier ON deliveries(courier_id);
CREATE INDEX IF NOT EXISTS idx_deliveries_status ON deliveries(status);
CREATE INDEX IF NOT EXISTS idx_deliveries_pickup_location ON deliveries USING GIST(pickup_location);
CREATE INDEX IF NOT EXISTS idx_deliveries_dropoff_location ON deliveries USING GIST(dropoff_location);
CREATE INDEX IF NOT EXISTS idx_tracking_delivery ON tracking_points(delivery_id);
CREATE INDEX IF NOT EXISTS idx_tracking_location ON tracking_points USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_tracking_timestamp ON tracking_points(timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_routes_delivery ON routes(delivery_id);
