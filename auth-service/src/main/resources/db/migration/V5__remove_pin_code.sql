-- ============================================================
-- V5: Loại bỏ cột pin_code trong bảng users
-- ============================================================
SET search_path TO auth;

ALTER TABLE users DROP COLUMN IF EXISTS pin_code;
