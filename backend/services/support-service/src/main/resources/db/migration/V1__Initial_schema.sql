-- ============================================
-- Support Service - Initial Schema
-- ============================================

-- Table: support_tickets
CREATE TABLE IF NOT EXISTS support_tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(50) UNIQUE NOT NULL,
    user_id BIGINT NOT NULL, -- Reference to auth-service users (logical, no FK)
    order_id BIGINT, -- Reference to order-service (logical, no FK)
    type VARCHAR(50), -- ORDER_ISSUE, PAYMENT, DELIVERY, ACCOUNT, TECHNICAL
    priority VARCHAR(50) DEFAULT 'MEDIUM', -- LOW, MEDIUM, HIGH, URGENT
    status VARCHAR(50) DEFAULT 'OPEN', -- OPEN, IN_PROGRESS, RESOLVED, CLOSED
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    assigned_to BIGINT, -- Reference to admin user (logical, no FK)
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

-- Table: ticket_messages
CREATE TABLE IF NOT EXISTS ticket_messages (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL, -- Reference to auth-service users (logical, no FK)
    sender_type VARCHAR(50) NOT NULL, -- CUSTOMER, ADMIN, SYSTEM
    message TEXT NOT NULL,
    attachments JSONB, -- Array of attachment URLs
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_read BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_message_ticket FOREIGN KEY (ticket_id) REFERENCES support_tickets(id) ON DELETE CASCADE
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_tickets_ticket_number ON support_tickets(ticket_number);
CREATE INDEX IF NOT EXISTS idx_tickets_user ON support_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_tickets_order ON support_tickets(order_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON support_tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_priority ON support_tickets(priority);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned ON support_tickets(assigned_to);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON support_tickets(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_ticket ON ticket_messages(ticket_id);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON ticket_messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_timestamp ON ticket_messages(timestamp DESC);
