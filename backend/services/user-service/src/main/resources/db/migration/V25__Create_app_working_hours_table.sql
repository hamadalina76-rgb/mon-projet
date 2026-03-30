-- ============================================================================
-- V25 : Horaires de travail globaux de l'application
-- ============================================================================

CREATE TABLE IF NOT EXISTS app_working_hours (
    id           BIGSERIAL    PRIMARY KEY,
    day_of_week  VARCHAR(10)  NOT NULL UNIQUE,  -- MONDAY..SUNDAY
    is_open      BOOLEAN      NOT NULL DEFAULT TRUE,
    open_time    TIME,                           -- NULL si is_open = false
    close_time   TIME,                           -- NULL si is_open = false
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Seed par défaut : lundi-samedi 08:00-22:00, dimanche fermé
INSERT INTO app_working_hours (day_of_week, is_open, open_time, close_time) VALUES
    ('MONDAY',    TRUE,  '08:00', '22:00'),
    ('TUESDAY',   TRUE,  '08:00', '22:00'),
    ('WEDNESDAY', TRUE,  '08:00', '22:00'),
    ('THURSDAY',  TRUE,  '08:00', '22:00'),
    ('FRIDAY',    TRUE,  '08:00', '22:00'),
    ('SATURDAY',  TRUE,  '08:00', '22:00'),
    ('SUNDAY',    FALSE, NULL,    NULL)
ON CONFLICT (day_of_week) DO NOTHING;
