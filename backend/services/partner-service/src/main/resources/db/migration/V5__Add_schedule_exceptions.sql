-- Jours fériés et dates exceptionnelles (fermetures) par partenaire
-- Format JSON: [{"date":"2025-12-25","label":"Noël","type":"CLOSED"}, ...]
ALTER TABLE partners ADD COLUMN IF NOT EXISTS schedule_exceptions_json TEXT;
