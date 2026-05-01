-- ============================================================
-- V4: Fix passwords because V3 used an invalid hallucinated hash
-- ============================================================
SET search_path TO auth;

UPDATE users SET password = '$2b$10$IrRNAwY7mesOZbo1SC.6VuIIUCh0fR.hKFzN9OIa8sOUsSdNgMipSa';
