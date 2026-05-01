-- Migration to add PayOS specific fields to orders table
-- Version: V2

ALTER TABLE orders.orders 
ADD COLUMN payos_order_code BIGINT;

COMMENT ON COLUMN orders.orders.payos_order_code IS 'Ma tham chieu thanh toan tu he thong PayOS';
