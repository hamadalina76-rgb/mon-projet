-- ============================================
-- Support Service - Add Missing Columns
-- ============================================

-- ==================== SUPPORT_TICKETS TABLE ====================

-- Rename type column to category if needed (compatibility)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'support_tickets' AND column_name = 'type') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'support_tickets' AND column_name = 'category') THEN
        ALTER TABLE support_tickets RENAME COLUMN type TO category;
    END IF;
END $$;

-- Add missing columns to support_tickets table
ALTER TABLE support_tickets
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(50),
    ADD COLUMN IF NOT EXISTS category VARCHAR(50),
    ADD COLUMN IF NOT EXISTS assigned_to_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS first_response_at TIMESTAMP;

-- Set category from type if category is null and type exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'support_tickets' AND column_name = 'type') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'support_tickets' AND column_name = 'category') THEN
        UPDATE support_tickets 
        SET category = type 
        WHERE category IS NULL AND type IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_support_tickets_user_type ON support_tickets(user_type);
CREATE INDEX IF NOT EXISTS idx_support_tickets_category ON support_tickets(category);

-- ==================== TICKET_MESSAGES TABLE ====================

-- Convert attachments JSONB to attachments_json TEXT if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'ticket_messages' AND column_name = 'attachments' 
               AND data_type = 'jsonb') THEN
        -- Add new column
        ALTER TABLE ticket_messages ADD COLUMN IF NOT EXISTS attachments_json TEXT;
        
        -- Copy data from JSONB to TEXT
        UPDATE ticket_messages 
        SET attachments_json = attachments::TEXT 
        WHERE attachments IS NOT NULL AND attachments_json IS NULL;
        
        -- Drop old column
        ALTER TABLE ticket_messages DROP COLUMN IF EXISTS attachments;
    END IF;
END $$;

-- Rename timestamp column to created_at if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'ticket_messages' AND column_name = 'timestamp') 
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns 
                      WHERE table_name = 'ticket_messages' AND column_name = 'created_at') THEN
        ALTER TABLE ticket_messages RENAME COLUMN timestamp TO created_at;
    END IF;
END $$;

-- Add missing columns to ticket_messages table
ALTER TABLE ticket_messages
    ADD COLUMN IF NOT EXISTS attachments_json TEXT,
    ADD COLUMN IF NOT EXISTS sender_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS is_internal BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Copy data from timestamp to created_at if needed
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns 
               WHERE table_name = 'ticket_messages' AND column_name = 'timestamp') 
       AND EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name = 'ticket_messages' AND column_name = 'created_at') THEN
        UPDATE ticket_messages 
        SET created_at = timestamp 
        WHERE created_at IS NULL AND timestamp IS NOT NULL;
    END IF;
END $$;

-- Add indexes for new columns
CREATE INDEX IF NOT EXISTS idx_ticket_messages_created_at ON ticket_messages(created_at DESC);
