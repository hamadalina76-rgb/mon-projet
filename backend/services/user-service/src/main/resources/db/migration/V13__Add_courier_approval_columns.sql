-- V13: Add courier approval flow columns (rejection_reason, request_more_info_message, suspension_reason)
-- Required for admin courier approval: reject with reason, request more info, suspend with reason

ALTER TABLE couriers
    ADD COLUMN IF NOT EXISTS rejection_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS request_more_info_message VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS suspension_reason VARCHAR(1000);

COMMENT ON COLUMN couriers.rejection_reason IS 'Raison du rejet (si status = REJECTED)';
COMMENT ON COLUMN couriers.request_more_info_message IS 'Message de demande d''informations complémentaires envoyé par l''admin';
COMMENT ON COLUMN couriers.suspension_reason IS 'Raison de la suspension (si status = SUSPENDED)';
