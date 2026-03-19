-- V24: Table d'association partenaires <-> zones de livraison
-- Les zone_id référencent la table adm_zones du location-service (différente DB → pas de FK)

CREATE TABLE IF NOT EXISTS partner_zones (
    id          BIGSERIAL PRIMARY KEY,
    partner_id  BIGINT      NOT NULL,
    zone_id     BIGINT      NOT NULL,
    assigned_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pz_partner  FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE CASCADE,
    CONSTRAINT uq_partner_zone UNIQUE (partner_id, zone_id)
);

CREATE INDEX IF NOT EXISTS idx_partner_zones_partner ON partner_zones(partner_id);
CREATE INDEX IF NOT EXISTS idx_partner_zones_zone    ON partner_zones(zone_id);
