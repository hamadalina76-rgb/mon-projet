-- ============================================
-- Delivery Service - Add Missing Columns
-- ============================================

-- ==================== DELIVERIES TABLE ====================

-- Add missing columns to deliveries table
ALTER TABLE deliveries
    ADD COLUMN IF NOT EXISTS order_number VARCHAR(50),
    ADD COLUMN IF NOT EXISTS courier_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS courier_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS pickup_latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS pickup_longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS dropoff_latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS dropoff_longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS estimated_distance DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS actual_distance DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS assigned_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arrived_at_pickup_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS picked_up_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS in_transit_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS arrived_at_dropoff_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS proof_of_delivery_image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS customer_signature VARCHAR(500),
    ADD COLUMN IF NOT EXISTS delivery_code VARCHAR(10),
    ADD COLUMN IF NOT EXISTS delivery_notes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS delivery_fee DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS tip DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS courier_earnings DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(50);

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename pickup_time to picked_up_at if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'deliveries' AND column_name = 'pickup_time') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'deliveries' AND column_name = 'picked_up_at') THEN
        ALTER TABLE deliveries RENAME COLUMN pickup_time TO picked_up_at;
    END IF;
    
    -- Rename dropoff_time to delivered_at if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'deliveries' AND column_name = 'dropoff_time') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'deliveries' AND column_name = 'delivered_at') THEN
        ALTER TABLE deliveries RENAME COLUMN dropoff_time TO delivered_at;
    END IF;
    
    -- Rename distance to actual_distance if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'deliveries' AND column_name = 'distance') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'deliveries' AND column_name = 'actual_distance') THEN
        ALTER TABLE deliveries RENAME COLUMN distance TO actual_distance;
    END IF;
    
    -- Rename proof_of_delivery to proof_of_delivery_image if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'deliveries' AND column_name = 'proof_of_delivery') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'deliveries' AND column_name = 'proof_of_delivery_image') THEN
        ALTER TABLE deliveries RENAME COLUMN proof_of_delivery TO proof_of_delivery_image;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_deliveries_accepted_at ON deliveries(accepted_at);
CREATE INDEX IF NOT EXISTS idx_deliveries_assigned_at ON deliveries(assigned_at);
CREATE INDEX IF NOT EXISTS idx_deliveries_delivered_at ON deliveries(delivered_at);
CREATE INDEX IF NOT EXISTS idx_deliveries_cancelled_at ON deliveries(cancelled_at);
CREATE INDEX IF NOT EXISTS idx_deliveries_delivery_code ON deliveries(delivery_code);

-- ==================== TRACKING_POINTS TABLE ====================

-- Add missing columns to tracking_points table
ALTER TABLE tracking_points
    ADD COLUMN IF NOT EXISTS latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS altitude DECIMAL(8,2),
    ADD COLUMN IF NOT EXISTS battery_level INTEGER,
    ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(50);

-- Update accuracy precision if needed (from DECIMAL(5,2) to DECIMAL(6,2))
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'tracking_points' 
               AND column_name = 'accuracy' 
               AND numeric_precision < 6) THEN
        ALTER TABLE tracking_points ALTER COLUMN accuracy TYPE DECIMAL(6,2);
    END IF;
END $$;

-- Populate latitude and longitude from location GEOGRAPHY if location exists and lat/long are null
-- Note: This extracts coordinates from PostGIS GEOGRAPHY column
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'tracking_points' AND column_name = 'location')
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'tracking_points' AND column_name = 'latitude')
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'tracking_points' AND column_name = 'longitude') THEN
        UPDATE tracking_points 
        SET latitude = ST_Y(location::geometry)::DECIMAL(10,8),
            longitude = ST_X(location::geometry)::DECIMAL(11,8)
        WHERE latitude IS NULL OR longitude IS NULL
          AND location IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_tracking_points_latitude ON tracking_points(latitude);
CREATE INDEX IF NOT EXISTS idx_tracking_points_longitude ON tracking_points(longitude);
CREATE INDEX IF NOT EXISTS idx_tracking_points_delivery_status ON tracking_points(delivery_status);
