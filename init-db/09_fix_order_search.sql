-- Thêm thông tin khách hàng vãng lai vào bảng orders (phục vụ Takeaway/Delivery CRM)
ALTER TABLE orders.orders ADD COLUMN IF NOT EXISTS customer_name VARCHAR(100);
ALTER TABLE orders.orders ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(20);

-- Tạo index trên customer_phone để tối ưu tốc độ tìm kiếm
CREATE INDEX IF NOT EXISTS idx_orders_customer_phone ON orders.orders(customer_phone);
