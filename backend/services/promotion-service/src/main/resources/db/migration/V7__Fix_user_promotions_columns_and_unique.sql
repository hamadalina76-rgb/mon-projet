-- V7: Fix user_promotions — rename columns + add unique constraint for upsertUsage

-- Rename first_used → first_used_at (if old column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_promotions' AND column_name = 'first_used')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_promotions' AND column_name = 'first_used_at')
    THEN
        ALTER TABLE user_promotions RENAME COLUMN first_used TO first_used_at;
    END IF;
END $$;

-- Rename last_used → last_used_at (if old column exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_promotions' AND column_name = 'last_used')
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'user_promotions' AND column_name = 'last_used_at')
    THEN
        ALTER TABLE user_promotions RENAME COLUMN last_used TO last_used_at;
    END IF;
END $$;

-- Drop the existing non-unique index
DROP INDEX IF EXISTS idx_user_promotions_user_promotion;

-- Create a UNIQUE constraint (required by ON CONFLICT in upsertUsage)
ALTER TABLE user_promotions
    ADD CONSTRAINT uq_user_promotions_user_promotion UNIQUE (user_id, promotion_id);
