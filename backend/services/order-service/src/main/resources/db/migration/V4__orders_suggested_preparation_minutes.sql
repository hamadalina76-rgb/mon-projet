-- Max des temps de préparation produit (minutes) pour la commande entière
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS suggested_preparation_minutes INTEGER;
