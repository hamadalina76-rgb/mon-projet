-- Ajout de la colonne service_fee pour les frais de service par zone
ALTER TABLE adm_zones ADD COLUMN IF NOT EXISTS service_fee NUMERIC(10, 2) DEFAULT 0;
