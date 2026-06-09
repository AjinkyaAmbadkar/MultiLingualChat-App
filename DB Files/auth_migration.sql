-- =============================================================================
-- Auth Migration — Phase 1
-- Run this BEFORE restarting the Spring Boot app with the new User entity.
-- Assumes the initial_DB_setup.sql has already been run (users table exists).
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Extend the users table with auth-related columns
-- -----------------------------------------------------------------------------

-- password_hash: nullable because Google OAuth users don't have a password.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

-- provider: which auth method the user registered with.
-- DEFAULT 'LOCAL' ensures existing rows get a valid value immediately.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

-- google_id: the unique ID returned by Google on OAuth2 login. Null for local users.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);

-- is_verified: email verification flag. False until user clicks the verification link.
-- Existing users default to false — you may want to manually set these to true
-- if you trust your existing user data.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS is_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- updated_at: auto-set by JPA @PreUpdate. Backfill with NOW() for existing rows.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Backfill updated_at for any existing rows (NOT NULL constraint added after backfill).
UPDATE users SET updated_at = NOW() WHERE updated_at IS NULL;

ALTER TABLE users
    ALTER COLUMN updated_at SET NOT NULL;

-- created_at already exists from initial setup, but ensure it has a default for safety.
ALTER TABLE users
    ALTER COLUMN created_at SET DEFAULT NOW();


-- -----------------------------------------------------------------------------
-- 2. Create the refresh_tokens table
-- -----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGSERIAL       PRIMARY KEY,

    -- Foreign key to users. ON DELETE CASCADE: if a user is deleted,
    -- all their tokens are automatically deleted too. No orphan rows.
    user_id     BIGINT          NOT NULL
                                REFERENCES users(id) ON DELETE CASCADE,

    -- The actual token string (UUID or signed string). Must be globally unique.
    token       VARCHAR(512)    NOT NULL UNIQUE,

    -- When this token expires. The auth service checks this before accepting a refresh.
    expires_at  TIMESTAMP       NOT NULL,

    -- Explicit revocation (logout). Revoked tokens are rejected even if not expired.
    revoked     BOOLEAN         NOT NULL DEFAULT FALSE,

    created_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);


-- -----------------------------------------------------------------------------
-- 3. Indexes for fast lookups
-- -----------------------------------------------------------------------------

-- token lookup: the most frequent query — "is this refresh token valid?"
-- Without an index this is a full table scan; with it, it's O(log n).
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token
    ON refresh_tokens(token);

-- user_id lookup: used when revoking all tokens for a user (logout / password change).
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id
    ON refresh_tokens(user_id);
