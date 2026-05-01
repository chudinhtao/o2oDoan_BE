-- ============================================================
-- V6__Add_Server_Role.sql
-- Phase 1: Thêm role SERVER và các trường cần thiết cho
-- tính năng điều phối phục vụ (Server Role)
-- ============================================================

-- 1. Cập nhật CHECK constraint cho role trong auth schema
ALTER TABLE auth.users DROP CONSTRAINT IF EXISTS users_role_check;
ALTER TABLE auth.users ADD CONSTRAINT users_role_check
    CHECK (role IN ('ADMIN', 'CASHIER', 'KITCHEN', 'SERVER'));

-- 2. Thêm cột served_at vào order_ticket_items
--    (completed_at, served_by, cancelled_by đã có sẵn từ Phase 1)
ALTER TABLE orders.order_ticket_items
    ADD COLUMN IF NOT EXISTS served_at TIMESTAMP;

-- 3. Thêm trường accepted_by và accepted_at vào staff_calls
--    (resolved_by, resolved_at đã có sẵn từ Phase 1)
ALTER TABLE orders.staff_calls
    ADD COLUMN IF NOT EXISTS accepted_by UUID REFERENCES auth.users(id),
    ADD COLUMN IF NOT EXISTS accepted_at TIMESTAMP;

-- 4. Cập nhật CHECK constraint status của staff_calls để bao gồm ACCEPTED
ALTER TABLE orders.staff_calls DROP CONSTRAINT IF EXISTS staff_calls_status_check;
ALTER TABLE orders.staff_calls ADD CONSTRAINT staff_calls_status_check
    CHECK (status IN ('PENDING', 'ACCEPTED', 'RESOLVED'));

-- 5. Index hỗ trợ Sweeper Job: tìm nhanh các món DONE chưa SERVED
CREATE INDEX IF NOT EXISTS idx_ticket_items_status_done
    ON orders.order_ticket_items (status, created_at)
    WHERE status = 'DONE';

-- 6. Index hỗ trợ Sweeper Job: tìm nhanh các StaffCall PENDING lâu
CREATE INDEX IF NOT EXISTS idx_staff_calls_pending
    ON orders.staff_calls (status, created_at)
    WHERE status = 'PENDING';
