-- V23 – Plannings exceptionnels des livreurs
-- Un planning exceptionnel (ponctuel) écrase le planning normal
-- sur une période définie (jours fériés, événements spéciaux, congé…)

CREATE TABLE IF NOT EXISTS courier_exceptional_schedules
(
    id             BIGSERIAL    PRIMARY KEY,
    courier_id     BIGINT       NOT NULL,
    courier_name   VARCHAR(150),

    -- JOUR_FERIE | EVENEMENT_SPECIAL | CONGE | FERMETURE | FORMATION
    exception_type VARCHAR(30)  NOT NULL,
    label          VARCHAR(200) NOT NULL,

    start_date     DATE         NOT NULL,
    end_date       DATE         NOT NULL,
    reason         TEXT,

    -- true  = le livreur ne travaille PAS (période de repos)
    -- false = des shifts spéciaux s'appliquent (version future)
    is_rest_period BOOLEAN      NOT NULL DEFAULT TRUE,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,

    admin_id       BIGINT,
    admin_name     VARCHAR(150),

    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ces_courier_id ON courier_exceptional_schedules (courier_id);
CREATE INDEX IF NOT EXISTS idx_ces_dates      ON courier_exceptional_schedules (start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_ces_active     ON courier_exceptional_schedules (is_active);
