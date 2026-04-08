-- Temps de préparation (minutes) issu de la fiche produit au moment de la commande
ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS preparation_time_min INTEGER;
