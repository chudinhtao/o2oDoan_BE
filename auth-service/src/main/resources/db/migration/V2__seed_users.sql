-- ============================================================
-- V2: Seed data — tài khoản mặc định
-- Password cho tất cả: "123456" (BCrypt, cost=10)
-- BCrypt hash của "123456":
--   $2b$10$IrRNAwY7mesOZbo1SC.6VuIIUCh0fR.hKFzN9OIa8sOUsSdNgMipSa
-- ============================================================
SET search_path TO auth;

INSERT INTO users (username, password, role, full_name) VALUES
    ('admin',    '$2b$10$IrRNAwY7mesOZbo1SC.6VuIIUCh0fR.hKFzN9OIa8sOUsSdNgMipSa', 'ADMIN',   'Quản lý'),
    ('cashier1', '$2b$10$IrRNAwY7mesOZbo1SC.6VuIIUCh0fR.hKFzN9OIa8sOUsSdNgMipSa', 'CASHIER', 'Thu ngân 1'),
    ('kitchen1', '$2b$10$IrRNAwY7mesOZbo1SC.6VuIIUCh0fR.hKFzN9OIa8sOUsSdNgMipSa', 'KITCHEN', 'Bếp 1');
