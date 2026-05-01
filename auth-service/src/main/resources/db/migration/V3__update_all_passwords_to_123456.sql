-- ============================================================
-- V3: Update passwords to '123456'
-- ============================================================
SET search_path TO auth;

UPDATE users SET password = '$2a$10$wK8gMy6O1/gVIn5OaH1.fublK9r.oF2iZ1A2.r4/DqkL6C3C0z3sC';
