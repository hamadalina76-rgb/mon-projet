-- DISP-205: grouped dispatch config, audit, simulation, replay, exclusivity

CREATE TABLE IF NOT EXISTS dispatch_config_version (
    id              BIGSERIAL PRIMARY KEY,
    version         BIGINT       NOT NULL UNIQUE,
    optimistic_lock INTEGER      NOT NULL DEFAULT 1,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by_user_id  VARCHAR(64),
    created_by_email    VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS dispatch_config_snapshot (
    version              BIGINT PRIMARY KEY REFERENCES dispatch_config_version (version) ON DELETE CASCADE,
    general_json         JSONB    NOT NULL DEFAULT '{}'::jsonb,
    scoring_json         JSONB    NOT NULL DEFAULT '{}'::jsonb,
    internal_external_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    bundling_json        JSONB    NOT NULL DEFAULT '{}'::jsonb,
    exclusivity_json     JSONB    NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS dispatch_config_active (
    id          INTEGER PRIMARY KEY CHECK (id = 1),
    version     BIGINT NOT NULL REFERENCES dispatch_config_version (version),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS dispatch_config_audit (
    id              BIGSERIAL PRIMARY KEY,
    occurred_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_user_id   VARCHAR(64),
    actor_email     VARCHAR(255),
    http_method     VARCHAR(16),
    config_group    VARCHAR(64) NOT NULL,
    endpoint_path   VARCHAR(512),
    diff_summary    TEXT,
    config_version  BIGINT NOT NULL REFERENCES dispatch_config_version (version)
);

CREATE INDEX IF NOT EXISTS idx_dispatch_config_audit_occurred
    ON dispatch_config_audit (occurred_at DESC);

CREATE TABLE IF NOT EXISTS dispatch_simulation_run (
    id              BIGSERIAL PRIMARY KEY,
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at    TIMESTAMPTZ,
    actor_user_id   VARCHAR(64),
    actor_email     VARCHAR(255),
    zone_id         BIGINT,
    overlay_json    JSONB NOT NULL,
    result_json     JSONB,
    status          VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS dispatch_cycle_capture (
    id                   BIGSERIAL PRIMARY KEY,
    external_cycle_id    UUID NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    zone_id              BIGINT NOT NULL,
    captured_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    pending_orders_json  JSONB,
    couriers_json        JSONB,
    pool_internal_only   BOOLEAN,
    config_version       BIGINT
);

CREATE INDEX IF NOT EXISTS idx_dispatch_cycle_capture_zone
    ON dispatch_cycle_capture (zone_id, captured_at DESC);

CREATE TABLE IF NOT EXISTS dispatch_replay_session (
    id                 BIGSERIAL PRIMARY KEY,
    cycle_capture_id   BIGINT NOT NULL REFERENCES dispatch_cycle_capture (id) ON DELETE CASCADE,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actor_user_id      VARCHAR(64),
    pair_count         INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dispatch_replay_pair_score (
    id                  BIGSERIAL PRIMARY KEY,
    replay_session_id   BIGINT NOT NULL REFERENCES dispatch_replay_session (id) ON DELETE CASCADE,
    order_id            BIGINT NOT NULL,
    courier_id          BIGINT NOT NULL,
    total_cost          DOUBLE PRECISION,
    eliminated          BOOLEAN NOT NULL DEFAULT FALSE,
    elimination_reason  VARCHAR(512),
    components_json     JSONB NOT NULL DEFAULT '[]'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_dispatch_replay_pair_session
    ON dispatch_replay_pair_score (replay_session_id);

CREATE TABLE IF NOT EXISTS dispatch_exclusivity_cell (
    zone_id         BIGINT NOT NULL,
    commerce_type   VARCHAR(64) NOT NULL,
    allowed         BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (zone_id, commerce_type)
);

CREATE TABLE IF NOT EXISTS dispatch_exclusivity_partner_override (
    id              BIGSERIAL PRIMARY KEY,
    partner_id      BIGINT NOT NULL,
    zone_id         BIGINT NOT NULL,
    commerce_type   VARCHAR(64) NOT NULL,
    allowed         BOOLEAN NOT NULL,
    UNIQUE (partner_id, zone_id, commerce_type)
);

-- Bootstrap row so GET never 404s before first admin save
INSERT INTO dispatch_config_version (version, optimistic_lock, created_by_user_id, created_by_email)
VALUES (1, 1, 'system', 'bootstrap@speedline')
ON CONFLICT (version) DO NOTHING;

INSERT INTO dispatch_config_snapshot (version, general_json, scoring_json, internal_external_json, bundling_json, exclusivity_json)
VALUES (1, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb, '{}'::jsonb)
ON CONFLICT (version) DO NOTHING;

INSERT INTO dispatch_config_active (id, version)
VALUES (1, 1)
ON CONFLICT (id) DO NOTHING;
