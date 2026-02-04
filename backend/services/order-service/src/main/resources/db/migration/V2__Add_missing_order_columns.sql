-- ============================================
-- Order Service - Add Missing Columns
-- ============================================

-- ==================== ORDERS TABLE ====================

-- Add missing columns to orders table
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS partner_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS partner_address VARCHAR(500),
    ADD COLUMN IF NOT EXISTS partner_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS courier_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS courier_phone VARCHAR(20),
    ADD COLUMN IF NOT EXISTS tip DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS delivery_address_json TEXT,
    ADD COLUMN IF NOT EXISTS delivery_latitude DECIMAL(10,8),
    ADD COLUMN IF NOT EXISTS delivery_longitude DECIMAL(11,8),
    ADD COLUMN IF NOT EXISTS payment_id BIGINT,
    ADD COLUMN IF NOT EXISTS is_scheduled BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scheduled_delivery_time TIMESTAMP,
    ADD COLUMN IF NOT EXISTS customer_notes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS internal_notes VARCHAR(500),
    ADD COLUMN IF NOT EXISTS cancelled_by VARCHAR(50);

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename delivery_address JSONB to delivery_address_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'orders' AND column_name = 'delivery_address' 
               AND data_type = 'jsonb') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'orders' AND column_name = 'delivery_address_json') THEN
        -- Copy data from JSONB to TEXT before renaming
        ALTER TABLE orders ADD COLUMN delivery_address_json_temp TEXT;
        UPDATE orders SET delivery_address_json_temp = delivery_address::TEXT WHERE delivery_address IS NOT NULL;
        ALTER TABLE orders DROP COLUMN delivery_address;
        ALTER TABLE orders RENAME COLUMN delivery_address_json_temp TO delivery_address_json;
    END IF;
END $$;

-- Extract latitude and longitude from delivery_location GEOGRAPHY if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'orders' AND column_name = 'delivery_location') THEN
        UPDATE orders 
        SET delivery_latitude = ST_Y(delivery_location::geometry)::DECIMAL(10,8),
            delivery_longitude = ST_X(delivery_location::geometry)::DECIMAL(11,8)
        WHERE (delivery_latitude IS NULL OR delivery_longitude IS NULL)
          AND delivery_location IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_orders_customer_name ON orders(customer_name);
CREATE INDEX IF NOT EXISTS idx_orders_partner_name ON orders(partner_name);
CREATE INDEX IF NOT EXISTS idx_orders_payment_id ON orders(payment_id);
CREATE INDEX IF NOT EXISTS idx_orders_is_scheduled ON orders(is_scheduled);
CREATE INDEX IF NOT EXISTS idx_orders_scheduled_delivery_time ON orders(scheduled_delivery_time);

-- ==================== ORDER_ITEMS TABLE ====================

-- Add missing columns to order_items table
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS product_description TEXT,
    ADD COLUMN IF NOT EXISTS product_image VARCHAR(500),
    ADD COLUMN IF NOT EXISTS modifiers_total DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS selected_options_json TEXT,
    ADD COLUMN IF NOT EXISTS selected_addons_json TEXT;

-- Rename existing columns if needed (compatibilité)
DO $$
BEGIN
    -- Rename selected_options JSONB to selected_options_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'order_items' AND column_name = 'selected_options' 
               AND data_type = 'jsonb') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'order_items' AND column_name = 'selected_options_json') THEN
        ALTER TABLE order_items ADD COLUMN selected_options_json_temp TEXT;
        UPDATE order_items SET selected_options_json_temp = selected_options::TEXT WHERE selected_options IS NOT NULL;
        ALTER TABLE order_items DROP COLUMN selected_options;
        ALTER TABLE order_items RENAME COLUMN selected_options_json_temp TO selected_options_json;
    END IF;
    
    -- Rename selected_addons JSONB to selected_addons_json TEXT if needed
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'order_items' AND column_name = 'selected_addons' 
               AND data_type = 'jsonb') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'order_items' AND column_name = 'selected_addons_json') THEN
        ALTER TABLE order_items ADD COLUMN selected_addons_json_temp TEXT;
        UPDATE order_items SET selected_addons_json_temp = selected_addons::TEXT WHERE selected_addons IS NOT NULL;
        ALTER TABLE order_items DROP COLUMN selected_addons;
        ALTER TABLE order_items RENAME COLUMN selected_addons_json_temp TO selected_addons_json;
    END IF;
END $$;

-- Make product_name NOT NULL if it's currently nullable
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'order_items' 
               AND column_name = 'product_name' 
               AND is_nullable = 'YES') THEN
        UPDATE order_items SET product_name = 'Unknown Product' WHERE product_name IS NULL;
        ALTER TABLE order_items ALTER COLUMN product_name SET NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_order_items_product_image ON order_items(product_image);

-- ==================== ORDER_STATUS_HISTORY TABLE ====================

-- Add missing columns to order_status_history table
ALTER TABLE order_status_history
    ADD COLUMN IF NOT EXISTS previous_status VARCHAR(50),
    ADD COLUMN IF NOT EXISTS actor_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS actor_id BIGINT,
    ADD COLUMN IF NOT EXISTS notes TEXT,
    ADD COLUMN IF NOT EXISTS location VARCHAR(100);

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_order_status_history_actor_id ON order_status_history(actor_id);
CREATE INDEX IF NOT EXISTS idx_order_status_history_actor_type ON order_status_history(actor_type);
CREATE INDEX IF NOT EXISTS idx_order_status_history_previous_status ON order_status_history(previous_status);
