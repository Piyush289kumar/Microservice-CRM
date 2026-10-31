
-- ==========================================
-- V1: INITIAL AUTH SERVICE DATABASE SCHEMA
-- Database: PostgreSQL
-- ==========================================


-- ==========================================
-- 1. USERS TABLE
-- ==========================================

CREATE TABLE users (
    id UUID PRIMARY KEY,

    first_name VARCHAR(100) NOT NULL,

    last_name VARCHAR(100),

    email VARCHAR(320) NOT NULL,

    password_hash VARCHAR(100) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    email_verified_at TIMESTAMPTZ,

    failed_login_attempts INTEGER NOT NULL DEFAULT 0,

    locked_until TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL,

    updated_at TIMESTAMPTZ NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT uk_users_email UNIQUE (email),

    CONSTRAINT chk_users_status CHECK (
        status IN (
            'ACTIVE',
            'PENDING_VERIFICATION',
            'LOCKED',
            'DISABLED'
        )
    ),

    CONSTRAINT chk_failed_login_attempts CHECK (
        failed_login_attempts >= 0
    )
);


-- Case-insensitive email uniqueness as an additional safeguard.
CREATE UNIQUE INDEX idx_users_email_lower
ON users (LOWER(email));


-- ==========================================
-- 2. ROLES TABLE
-- ==========================================

CREATE TABLE roles (
    id UUID PRIMARY KEY,

    name VARCHAR(50) NOT NULL,

    description VARCHAR(255),

    CONSTRAINT uk_roles_name UNIQUE (name)
);


-- ==========================================
-- 3. USER ROLES MAPPING TABLE
-- ==========================================

CREATE TABLE user_roles (
    user_id UUID NOT NULL,

    role_id UUID NOT NULL,

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES roles(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_user_roles
        UNIQUE (user_id, role_id)
);


CREATE INDEX idx_user_roles_role_id
ON user_roles (role_id);


-- ==========================================
-- 4. REFRESH TOKENS TABLE
-- ==========================================

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY,

    user_id UUID NOT NULL,

    token_hash VARCHAR(64) NOT NULL,

    expires_at TIMESTAMPTZ NOT NULL,

    created_at TIMESTAMPTZ NOT NULL,

    revoked_at TIMESTAMPTZ,

    replaced_by_token_hash VARCHAR(64),

    CONSTRAINT uk_refresh_tokens_hash
        UNIQUE (token_hash),

    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);


CREATE INDEX idx_refresh_tokens_user_id
ON refresh_tokens (user_id);


CREATE INDEX idx_refresh_tokens_expires_at
ON refresh_tokens (expires_at);


-- ==========================================
-- 5. DEFAULT ROLE DATA
-- ==========================================

INSERT INTO roles (id, name, description)
VALUES
(
    gen_random_uuid(),
    'ROLE_USER',
    'Standard application user'
),
(
    gen_random_uuid(),
    'ROLE_ADMIN',
    'System administrator'
),
(
    gen_random_uuid(),
    'ROLE_MANAGER',
    'Application manager'
);
