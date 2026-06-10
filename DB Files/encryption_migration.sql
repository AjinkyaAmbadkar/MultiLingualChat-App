-- Phase 8.5 — Encryption-at-Rest migration
-- Run AFTER auth_migration.sql has been applied.
-- Dev note: drop + recreate messages to avoid migrating existing plaintext rows.

-- ── Users table: add key-storage columns ──────────────────────────────────────
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS public_key           TEXT,
    ADD COLUMN IF NOT EXISTS encrypted_private_key TEXT,
    ADD COLUMN IF NOT EXISTS key_salt             VARCHAR(64);

-- ── Messages table: swap plaintext for ciphertext columns ─────────────────────
-- Drop the old plaintext columns (no data migration — dev environment).
ALTER TABLE messages
    DROP COLUMN IF EXISTS original_text,
    DROP COLUMN IF EXISTS translated_text,
    DROP COLUMN IF EXISTS sender_translated_text;

-- Add encrypted columns.
-- Two separate IVs so (key, nonce) is unique for each plaintext field.
ALTER TABLE messages
    ADD COLUMN IF NOT EXISTS encrypted_original_text    TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS encrypted_translated_text  TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS encrypted_sender_text      TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS aes_key_for_sender         TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS aes_key_for_receiver       TEXT        NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS aes_iv_original            VARCHAR(32) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS aes_iv_translated          VARCHAR(32) NOT NULL DEFAULT '',
    ADD COLUMN IF NOT EXISTS aes_iv_sender              VARCHAR(32) NOT NULL DEFAULT '';

-- Remove the temporary defaults now that the columns exist.
ALTER TABLE messages
    ALTER COLUMN encrypted_original_text   DROP DEFAULT,
    ALTER COLUMN encrypted_translated_text DROP DEFAULT,
    ALTER COLUMN encrypted_sender_text     DROP DEFAULT,
    ALTER COLUMN aes_key_for_sender        DROP DEFAULT,
    ALTER COLUMN aes_key_for_receiver      DROP DEFAULT,
    ALTER COLUMN aes_iv_original           DROP DEFAULT,
    ALTER COLUMN aes_iv_translated         DROP DEFAULT,
    ALTER COLUMN aes_iv_sender             DROP DEFAULT;
