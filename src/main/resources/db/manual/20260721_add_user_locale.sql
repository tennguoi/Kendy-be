-- PostgreSQL migration: add locale column to users table
-- Allows per-user language preference for notifications and emails

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS locale VARCHAR(10);

-- Set default locale to 'vi' for existing users
UPDATE users
SET locale = 'vi'
WHERE locale IS NULL;
