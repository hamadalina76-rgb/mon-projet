-- ============================================
-- Location Service - Initial Schema
-- ============================================

-- Enable PostGIS extension
CREATE EXTENSION IF NOT EXISTS postgis;

-- Table: zones
CREATE TABLE IF NOT EXISTS zones (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    boundary GEOGRAPHY(POLYGON, 4326) NOT NULL,
    type VARCHAR(50), -- DELIVERY, RESTRICTED, PREMIUM
    delivery_fee DECIMAL(10,2),
    minimum_order DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_zones_boundary ON zones USING GIST(boundary);
CREATE INDEX IF NOT EXISTS idx_zones_type ON zones(type);
CREATE INDEX IF NOT EXISTS idx_zones_active ON zones(is_active);

-- Function: Find nearby partners (example function)
CREATE OR REPLACE FUNCTION find_nearby_partners(
    user_lat DOUBLE PRECISION,
    user_lon DOUBLE PRECISION,
    radius_meters INTEGER DEFAULT 5000
) RETURNS TABLE (
    partner_id BIGINT,
    distance_meters DOUBLE PRECISION
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        p.id AS partner_id,
        ST_Distance(
            p.location::geography,
            ST_SetSRID(ST_MakePoint(user_lon, user_lat), 4326)::geography
        ) AS distance_meters
    FROM partners p
    WHERE ST_DWithin(
        p.location::geography,
        ST_SetSRID(ST_MakePoint(user_lon, user_lat), 4326)::geography,
        radius_meters
    )
    AND p.is_active = TRUE
    AND p.accepts_orders = TRUE
    ORDER BY distance_meters
    LIMIT 50;
END;
$$ LANGUAGE plpgsql;
