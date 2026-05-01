-- V3: Add zone column to orders.tables
ALTER TABLE orders.tables
    ADD COLUMN IF NOT EXISTS zone VARCHAR(50);

COMMENT ON COLUMN orders.tables.zone IS 'Khu vực bàn (Tầng 1, Ban công, VIP, ...)';
