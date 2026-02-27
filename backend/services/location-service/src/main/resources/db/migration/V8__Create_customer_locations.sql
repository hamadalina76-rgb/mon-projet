-- V8: Table pour sauvegarder les adresses clients confirmées depuis l'app mobile
-- Stocke chaque adresse choisie via GPS ou saisie manuelle

CREATE TABLE IF NOT EXISTS customer_locations (
    id          BIGSERIAL PRIMARY KEY,
    user_id     VARCHAR(255) NOT NULL DEFAULT 'anonymous',
    formatted_address VARCHAR(500) NOT NULL,
    street      VARCHAR(255),
    city        VARCHAR(100),
    state       VARCHAR(100),
    postal_code VARCHAR(20),
    country     VARCHAR(100) DEFAULT 'Tunisie',
    latitude    DECIMAL(10, 7) NOT NULL,
    longitude   DECIMAL(10, 7) NOT NULL,
    address_type VARCHAR(50)  DEFAULT 'HOME',
    custom_label VARCHAR(100),
    is_default  BOOLEAN      DEFAULT FALSE,
    saved_at    TIMESTAMP    DEFAULT NOW(),
    created_at  TIMESTAMP    DEFAULT NOW()
);

-- Index pour chercher rapidement les adresses d'un utilisateur
CREATE INDEX IF NOT EXISTS idx_customer_locations_user_id
    ON customer_locations (user_id);

-- Index pour récupérer l'adresse par défaut d'un utilisateur
CREATE INDEX IF NOT EXISTS idx_customer_locations_user_default
    ON customer_locations (user_id, is_default);
