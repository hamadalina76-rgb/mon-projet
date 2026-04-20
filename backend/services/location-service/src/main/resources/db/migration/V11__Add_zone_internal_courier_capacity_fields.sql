-- Ajoute les nouveaux attributs de capacité/inter-zone à adm_zones
ALTER TABLE adm_zones
    ADD COLUMN IF NOT EXISTS min_active_internal_couriers INTEGER,
    ADD COLUMN IF NOT EXISTS max_simultaneous_orders INTEGER,
    ADD COLUMN IF NOT EXISTS inter_zone_extension_radius_km INTEGER,
    ADD COLUMN IF NOT EXISTS max_inter_zone_reassignment_delay_minutes INTEGER;

-- Affectations des livreurs internes par zone (avec horaires)
CREATE TABLE IF NOT EXISTS adm_zone_internal_courier_assignments (
    id BIGSERIAL PRIMARY KEY,
    zone_id BIGINT NOT NULL,
    courier_id BIGINT NOT NULL,
    start_time TIME,
    end_time TIME,
    CONSTRAINT fk_zone_internal_courier_zone
        FOREIGN KEY (zone_id) REFERENCES adm_zones(id) ON DELETE CASCADE,
    CONSTRAINT uk_zone_internal_courier_zone_courier
        UNIQUE (zone_id, courier_id)
);

CREATE INDEX IF NOT EXISTS idx_zone_internal_courier_zone
    ON adm_zone_internal_courier_assignments(zone_id);

CREATE INDEX IF NOT EXISTS idx_zone_internal_courier_courier
    ON adm_zone_internal_courier_assignments(courier_id);

-- Jours de travail par affectation
CREATE TABLE IF NOT EXISTS adm_zone_internal_courier_work_days (
    assignment_id BIGINT NOT NULL,
    work_day VARCHAR(20) NOT NULL,
    CONSTRAINT fk_zone_internal_courier_work_days_assignment
        FOREIGN KEY (assignment_id) REFERENCES adm_zone_internal_courier_assignments(id) ON DELETE CASCADE,
    CONSTRAINT uk_zone_internal_courier_work_days
        UNIQUE (assignment_id, work_day)
);

CREATE INDEX IF NOT EXISTS idx_zone_internal_courier_work_days_assignment
    ON adm_zone_internal_courier_work_days(assignment_id);
