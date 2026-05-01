-- ============================================================
-- V1: Tạo schema auth + bảng users và refresh_tokens
-- ============================================================
CREATE SCHEMA IF NOT EXISTS auth;
SET search_path TO auth;

CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,          -- BCrypt hash
    role        VARCHAR(20) NOT NULL
                    CHECK (role IN ('ADMIN','CASHIER','KITCHEN')),
    full_name   VARCHAR(100),
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) UNIQUE NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- Index để tìm nhanh theo username
CREATE INDEX idx_users_username ON users(username);
-- Index để tìm refresh token
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
