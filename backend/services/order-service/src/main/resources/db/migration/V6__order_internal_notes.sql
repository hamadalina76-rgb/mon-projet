-- V6: Internal notes table for admin order notes
CREATE TABLE IF NOT EXISTS order_internal_notes (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    content         VARCHAR(2000)   NOT NULL,
    visibility      VARCHAR(20)     NOT NULL DEFAULT 'ADMIN_ONLY',
    author_id       BIGINT          NOT NULL,
    author_name     VARCHAR(100)    NOT NULL,
    author_role     VARCHAR(50),
    created_at      TIMESTAMP       DEFAULT NOW(),

    CONSTRAINT fk_note_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_internal_note_order ON order_internal_notes(order_id);
CREATE INDEX IF NOT EXISTS idx_internal_note_created ON order_internal_notes(created_at);
