-- V20: Table des zones assignées aux livreurs par l'admin lors de l'approbation
-- Référence les IDs de zones gérées par location-service (pas de FK inter-service)

CREATE TABLE IF NOT EXISTS courier_assigned_zones (
    courier_id BIGINT NOT NULL,
    zone_id    BIGINT NOT NULL,
    PRIMARY KEY (courier_id, zone_id),
    CONSTRAINT fk_caz_courier FOREIGN KEY (courier_id)
        REFERENCES couriers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_courier_assigned_zones_courier_id ON courier_assigned_zones(courier_id);
