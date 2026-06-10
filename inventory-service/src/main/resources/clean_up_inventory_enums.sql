-- Script dọn dẹp các trạng thái rác (Enums cũ) trong Database Inventory
-- Chạy script này trực tiếp trên database `fnb_db` (hoặc schema `inventory`)

-- 1. Xóa bỏ PENDING và APPROVED trong purchase_orders
-- Chuyển PENDING -> DRAFT (Để nhân viên có thể xem lại và gửi/nhập sau)
UPDATE inventory.purchase_orders
SET status = 'DRAFT'
WHERE status IN ('PENDING', 'APPROVED');

-- 2. Hợp nhất IN_PROGRESS và REVIEW thành COUNTING trong stocktakes
UPDATE inventory.stocktakes
SET status = 'COUNTING'
WHERE status IN ('IN_PROGRESS', 'REVIEW');

-- Kiểm tra lại kết quả
-- SELECT id, po_number, status FROM inventory.purchase_orders;
-- SELECT id, name, status FROM inventory.stocktakes;
