-- V11: Add identity document columns to couriers table
-- Add new columns for CIN number and front/back images

-- Add identity_number column
ALTER TABLE couriers 
ADD COLUMN IF NOT EXISTS identity_number VARCHAR(50);

-- Add identity_document_front_image column
ALTER TABLE couriers 
ADD COLUMN IF NOT EXISTS identity_document_front_image VARCHAR(500);

-- Add identity_document_back_image column
ALTER TABLE couriers 
ADD COLUMN IF NOT EXISTS identity_document_back_image VARCHAR(500);

-- Remove old identity_document_image column if it exists
ALTER TABLE couriers 
DROP COLUMN IF EXISTS identity_document_image;

-- Add comments to document the columns
COMMENT ON COLUMN couriers.identity_number IS 'Numéro de la carte d''identité nationale (CIN)';
COMMENT ON COLUMN couriers.identity_document_front_image IS 'URL de la photo recto de la CIN';
COMMENT ON COLUMN couriers.identity_document_back_image IS 'URL de la photo verso de la CIN';
