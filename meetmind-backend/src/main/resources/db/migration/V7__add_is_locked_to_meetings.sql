-- Add is_locked to meetings table
ALTER TABLE meetings ADD COLUMN IF NOT EXISTS is_locked BOOLEAN NOT NULL DEFAULT FALSE;

