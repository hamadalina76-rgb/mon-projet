-- V15: Work schedule templates and courier schedule management
-- Templates apply to INTERNAL couriers only

-- ── Schedule Templates ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS schedule_templates (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ── Template Shifts (per day, multi-shift supported) ──────────────────────
CREATE TABLE IF NOT EXISTS schedule_template_shifts (
    id             BIGSERIAL PRIMARY KEY,
    template_id    BIGINT       NOT NULL REFERENCES schedule_templates(id) ON DELETE CASCADE,
    day_of_week    VARCHAR(10)  NOT NULL, -- MONDAY … SUNDAY
    shift_order    INT          NOT NULL DEFAULT 0,
    start_time     TIME         NOT NULL,
    end_time       TIME         NOT NULL,
    break_duration INT          DEFAULT 0, -- minutes
    CONSTRAINT uq_template_day_shift UNIQUE (template_id, day_of_week, shift_order)
);

-- ── Courier Schedules (can be template-based or manual) ───────────────────
CREATE TABLE IF NOT EXISTS courier_schedules (
    id              BIGSERIAL PRIMARY KEY,
    courier_id      BIGINT  NOT NULL,
    template_id     BIGINT  REFERENCES schedule_templates(id),
    week_start_date DATE,          -- NULL = permanent default schedule
    is_permanent    BOOLEAN DEFAULT TRUE,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_courier_week UNIQUE (courier_id, week_start_date)
);

-- ── Courier Schedule Shifts (actual daily shifts per schedule) ─────────────
CREATE TABLE IF NOT EXISTS courier_schedule_shifts (
    id             BIGSERIAL PRIMARY KEY,
    schedule_id    BIGINT      NOT NULL REFERENCES courier_schedules(id) ON DELETE CASCADE,
    day_of_week    VARCHAR(10) NOT NULL,
    shift_order    INT         NOT NULL DEFAULT 0,
    start_time     TIME        NOT NULL,
    end_time       TIME        NOT NULL,
    break_duration INT         DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_schedule_template_shifts_template ON schedule_template_shifts(template_id);
CREATE INDEX IF NOT EXISTS idx_courier_schedules_courier ON courier_schedules(courier_id);
CREATE INDEX IF NOT EXISTS idx_courier_schedule_shifts_schedule ON courier_schedule_shifts(schedule_id);
