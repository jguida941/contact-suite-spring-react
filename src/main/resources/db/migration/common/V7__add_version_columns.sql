-- Add version columns for optimistic locking (ADR-0044)
-- Prevents concurrent update race conditions (BACKEND-LOGIC-04)

ALTER TABLE contacts ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE tasks ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE appointments ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
