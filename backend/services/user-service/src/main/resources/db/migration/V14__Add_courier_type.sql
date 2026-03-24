-- V14: Add courier_type column to couriers table
-- INTERNAL = livreur employé de SpeedLine
-- EXTERNAL = livreur partenaire indépendant
ALTER TABLE couriers
    ADD COLUMN IF NOT EXISTS courier_type VARCHAR(20) DEFAULT NULL;

COMMENT ON COLUMN couriers.courier_type IS 'Type de livreur : INTERNAL (employé) ou EXTERNAL (indépendant). Défini lors de l''approbation.';
