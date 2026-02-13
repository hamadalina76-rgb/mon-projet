-- Fix Flyway V7 migration checksum mismatch
DROP TABLE IF EXISTS activity_logs CASCADE;
DELETE FROM flyway_schema_history WHERE version = '7';
