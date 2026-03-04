-- ============================================
-- Partner Service - Staff members table
-- ============================================

CREATE TABLE IF NOT EXISTS staff_members (
    id BIGSERIAL PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_partner FOREIGN KEY (partner_id) REFERENCES partners(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_staff_members_partner_id ON staff_members(partner_id);
CREATE INDEX IF NOT EXISTS idx_staff_members_user_id ON staff_members(user_id);

-- Backfill: one OWNER per existing partner (user_id from partners)
INSERT INTO staff_members (partner_id, user_id, role)
SELECT id, user_id, 'OWNER' FROM partners
WHERE user_id IS NOT NULL;
