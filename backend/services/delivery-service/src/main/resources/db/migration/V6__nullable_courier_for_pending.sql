-- Allow PENDING deliveries without an assigned courier yet
ALTER TABLE deliveries
    ALTER COLUMN courier_id DROP NOT NULL;
