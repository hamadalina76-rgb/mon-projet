-- ==================== SpeedLine Database Initialization ====================
-- This script creates all required databases and enables PostGIS extension

-- Create databases
CREATE DATABASE speedline_auth;
CREATE DATABASE speedline_users;
CREATE DATABASE speedline_partners;
CREATE DATABASE speedline_orders;
CREATE DATABASE speedline_delivery;
CREATE DATABASE speedline_payments;
CREATE DATABASE speedline_locations;
CREATE DATABASE speedline_promotions;
CREATE DATABASE speedline_support;

-- Enable PostGIS extension on databases that need geospatial features
\c speedline_users
CREATE EXTENSION IF NOT EXISTS postgis;

\c speedline_partners
CREATE EXTENSION IF NOT EXISTS postgis;

\c speedline_orders
CREATE EXTENSION IF NOT EXISTS postgis;

\c speedline_delivery
CREATE EXTENSION IF NOT EXISTS postgis;

\c speedline_locations
CREATE EXTENSION IF NOT EXISTS postgis;

-- Grant privileges
\c postgres
GRANT ALL PRIVILEGES ON DATABASE speedline_auth TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_users TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_partners TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_orders TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_delivery TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_payments TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_locations TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_promotions TO postgres;
GRANT ALL PRIVILEGES ON DATABASE speedline_support TO postgres;

-- ==================== SUCCESS ====================
-- All SpeedLine databases created successfully!
