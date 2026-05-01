ALTER TABLE orders.orders ADD COLUMN IF NOT EXISTS order_type VARCHAR(20) DEFAULT 'DINE_IN';
UPDATE orders.orders SET order_type = 'TAKEAWAY' WHERE table_id IS NULL;
