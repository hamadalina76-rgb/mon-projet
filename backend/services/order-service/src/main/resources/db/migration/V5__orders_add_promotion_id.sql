-- Ajout de la colonne promotion_id sur la table orders
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS promotion_id BIGINT;
