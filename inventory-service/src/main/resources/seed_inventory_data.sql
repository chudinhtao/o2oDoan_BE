-- Xóa dữ liệu cũ (nếu có) để tránh lỗi trùng lặp khi chạy lại
TRUNCATE TABLE inventory.stock_transactions CASCADE;
TRUNCATE TABLE inventory.stocktake_items CASCADE;
TRUNCATE TABLE inventory.stocktakes CASCADE;
TRUNCATE TABLE inventory.purchase_order_items CASCADE;
TRUNCATE TABLE inventory.purchase_orders CASCADE;
TRUNCATE TABLE inventory.recipe_items CASCADE;
TRUNCATE TABLE inventory.recipes CASCADE;
TRUNCATE TABLE inventory.item_uom_conversions CASCADE;
TRUNCATE TABLE inventory.inventory_levels CASCADE;
TRUNCATE TABLE inventory.inventory_items CASCADE;
TRUNCATE TABLE inventory.locations CASCADE;
TRUNCATE TABLE inventory.suppliers CASCADE;
TRUNCATE TABLE inventory.item_categories CASCADE;
TRUNCATE TABLE inventory.uoms CASCADE;

-- 1. UOMs (Đơn vị tính)
INSERT INTO inventory.uoms (id, name, short_name, created_at, created_by) VALUES
('e3b0c442-989b-464c-8693-123456789011', 'Kilogram', 'kg', NOW(), 'system'),
('e3b0c442-989b-464c-8693-123456789012', 'Gram', 'g', NOW(), 'system'),
('e3b0c442-989b-464c-8693-123456789013', 'Liter', 'l', NOW(), 'system'),
('e3b0c442-989b-464c-8693-123456789014', 'Milliliter', 'ml', NOW(), 'system'),
('e3b0c442-989b-464c-8693-123456789015', 'Box', 'box', NOW(), 'system'),
('e3b0c442-989b-464c-8693-123456789016', 'Piece', 'pcs', NOW(), 'system');

-- 2. Item Categories (Danh mục kho)
INSERT INTO inventory.item_categories (id, name, created_at, created_by) VALUES
('c0b0c442-989b-464c-8693-123456789021', 'Thịt & Hải sản', NOW(), 'system'),
('c0b0c442-989b-464c-8693-123456789022', 'Rau củ quả', NOW(), 'system'),
('c0b0c442-989b-464c-8693-123456789023', 'Nguyên liệu pha chế', NOW(), 'system'),
('c0b0c442-989b-464c-8693-123456789024', 'Bao bì & Dụng cụ', NOW(), 'system');

-- 3. Suppliers (Nhà cung cấp)
INSERT INTO inventory.suppliers (id, code, name, phone, is_active, created_at, created_by) VALUES
('51b0c442-989b-464c-8693-123456789031', 'SUP-VISSAN', 'Công ty Vissan', '0901234567', true, NOW(), 'system'),
('52b0c442-989b-464c-8693-123456789032', 'SUP-VINAMILK', 'Vinamilk', '0912345678', true, NOW(), 'system'),
('53b0c442-989b-464c-8693-123456789033', 'SUP-TRUNGUYEN', 'Cà phê Trung Nguyên', '0923456789', true, NOW(), 'system');

-- 4. Locations (Vị trí kho)
INSERT INTO inventory.locations (id, name, is_active, created_at, created_by) VALUES
('10b0c442-989b-464c-8693-123456789041', 'Kho Tổng', true, NOW(), 'system'),
('20b0c442-989b-464c-8693-123456789042', 'Kho Bếp chính', true, NOW(), 'system'),
('30b0c442-989b-464c-8693-123456789043', 'Quầy Pha chế', true, NOW(), 'system');

-- 5. Inventory Items (Mặt hàng tồn kho)
INSERT INTO inventory.inventory_items (id, sku, name, category_id, type, base_uom_id, safety_stock, avg_cost_price, is_active, created_at, created_by) VALUES
('11b0c442-989b-464c-8693-123456789051', 'RAW-BEEF', 'Thịt bò tươi', 'c0b0c442-989b-464c-8693-123456789021', 'RAW', 'e3b0c442-989b-464c-8693-123456789011', 5.0, 250000, true, NOW(), 'system'),
('22b0c442-989b-464c-8693-123456789052', 'RAW-COFFEE', 'Hạt cà phê Arabica', 'c0b0c442-989b-464c-8693-123456789023', 'RAW', 'e3b0c442-989b-464c-8693-123456789011', 10.0, 180000, true, NOW(), 'system'),
('33b0c442-989b-464c-8693-123456789053', 'RAW-MILK', 'Sữa tươi thanh trùng', 'c0b0c442-989b-464c-8693-123456789023', 'RAW', 'e3b0c442-989b-464c-8693-123456789013', 20.0, 35000, true, NOW(), 'system'),
('44b0c442-989b-464c-8693-123456789054', 'PACK-CUP', 'Ly giấy mang đi', 'c0b0c442-989b-464c-8693-123456789024', 'CONSUMABLE', 'e3b0c442-989b-464c-8693-123456789015', 50.0, 50000, true, NOW(), 'system');

-- 6. Item UOM Conversions (Quy đổi đơn vị tính)
INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by) VALUES
(gen_random_uuid(), '11b0c442-989b-464c-8693-123456789051', 'e3b0c442-989b-464c-8693-123456789011', 'e3b0c442-989b-464c-8693-123456789012', 1000, NOW(), 'system'), -- 1 kg Beef = 1000 g
(gen_random_uuid(), '22b0c442-989b-464c-8693-123456789052', 'e3b0c442-989b-464c-8693-123456789011', 'e3b0c442-989b-464c-8693-123456789012', 1000, NOW(), 'system'), -- 1 kg Coffee = 1000 g
(gen_random_uuid(), '33b0c442-989b-464c-8693-123456789053', 'e3b0c442-989b-464c-8693-123456789013', 'e3b0c442-989b-464c-8693-123456789014', 1000, NOW(), 'system'), -- 1 L Milk = 1000 ml
(gen_random_uuid(), '44b0c442-989b-464c-8693-123456789054', 'e3b0c442-989b-464c-8693-123456789015', 'e3b0c442-989b-464c-8693-123456789016', 100, NOW(), 'system'); -- 1 Box Cup = 100 pcs

-- 7. Inventory Levels (Tồn kho hiện tại)
INSERT INTO inventory.inventory_levels (id, item_id, location_id, current_stock, allocated_stock, created_at, created_by) VALUES
(gen_random_uuid(), '11b0c442-989b-464c-8693-123456789051', '10b0c442-989b-464c-8693-123456789041', 15.5, 0, NOW(), 'system'),
(gen_random_uuid(), '22b0c442-989b-464c-8693-123456789052', '10b0c442-989b-464c-8693-123456789041', 20.0, 0, NOW(), 'system'),
(gen_random_uuid(), '33b0c442-989b-464c-8693-123456789053', '10b0c442-989b-464c-8693-123456789041', 50.0, 0, NOW(), 'system'),
(gen_random_uuid(), '44b0c442-989b-464c-8693-123456789054', '10b0c442-989b-464c-8693-123456789041', 100.0, 0, NOW(), 'system');

-- 8. Recipes & Recipe Items (Công thức định lượng món ăn)
INSERT INTO inventory.recipes (id, sale_item_id, type, created_at, created_by) VALUES
('61b0c442-989b-464c-8693-123456789061', gen_random_uuid(), 'STANDARD', NOW(), 'system'),
('62b0c442-989b-464c-8693-123456789062', gen_random_uuid(), 'STANDARD', NOW(), 'system');

INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, created_at, created_by) VALUES
(gen_random_uuid(), '61b0c442-989b-464c-8693-123456789061', '22b0c442-989b-464c-8693-123456789052', 20, 'e3b0c442-989b-464c-8693-123456789012', 0, NOW(), 'system'), -- 20g Cà phê
(gen_random_uuid(), '61b0c442-989b-464c-8693-123456789061', '33b0c442-989b-464c-8693-123456789053', 30, 'e3b0c442-989b-464c-8693-123456789014', 0, NOW(), 'system'), -- 30ml Sữa
(gen_random_uuid(), '61b0c442-989b-464c-8693-123456789061', '44b0c442-989b-464c-8693-123456789054', 1, 'e3b0c442-989b-464c-8693-123456789016', 0, NOW(), 'system'), -- 1 pcs Cup
(gen_random_uuid(), '62b0c442-989b-464c-8693-123456789062', '11b0c442-989b-464c-8693-123456789051', 150, 'e3b0c442-989b-464c-8693-123456789012', 0, NOW(), 'system'); -- 150g Thịt bò

-- 9. Purchase Orders (Đơn nhập kho)
INSERT INTO inventory.purchase_orders (id, po_number, supplier_id, status, type, total_amount, created_at, created_by) VALUES
('71b0c442-989b-464c-8693-123456789071', 'PO-2026-0001', '51b0c442-989b-464c-8693-123456789031', 'COMPLETED', 'STANDARD', 3750000, NOW(), 'system'),
('72b0c442-989b-464c-8693-123456789072', 'PO-2026-0002', '52b0c442-989b-464c-8693-123456789032', 'DRAFT', 'STANDARD', 1750000, NOW(), 'system');

INSERT INTO inventory.purchase_order_items (id, po_id, item_id, quantity, uom_id, unit_price, created_at, created_by) VALUES
(gen_random_uuid(), '71b0c442-989b-464c-8693-123456789071', '11b0c442-989b-464c-8693-123456789051', 15.0, 'e3b0c442-989b-464c-8693-123456789011', 250000, NOW(), 'system'),
(gen_random_uuid(), '72b0c442-989b-464c-8693-123456789072', '33b0c442-989b-464c-8693-123456789053', 50.0, 'e3b0c442-989b-464c-8693-123456789013', 35000, NOW(), 'system');

-- 10. Stocktakes (Phiếu kiểm kê)
INSERT INTO inventory.stocktakes (id, snapshot_time, completed_at, status, created_at, created_by) VALUES
('81b0c442-989b-464c-8693-123456789081', NOW(), NOW(), 'COMPLETED', NOW(), 'system');

INSERT INTO inventory.stocktake_items (id, stocktake_id, item_id, system_quantity, counted_quantity, variance, adjustment_reason, created_at, created_by) VALUES
(gen_random_uuid(), '81b0c442-989b-464c-8693-123456789081', '11b0c442-989b-464c-8693-123456789051', 16.0, 15.5, -0.5, 'Hao hụt sơ chế', NOW(), 'system');

-- 11. Stock Transactions (Lịch sử xuất nhập kho)
INSERT INTO inventory.stock_transactions (id, item_id, transaction_type, quantity_change, reference_id, reason, unit_price_at_transaction, created_at, created_by) VALUES
(gen_random_uuid(), '11b0c442-989b-464c-8693-123456789051', 'IN_PO', 15.0, '71b0c442-989b-464c-8693-123456789071', 'Nhập kho từ PO', 250000, NOW(), 'system'),
(gen_random_uuid(), '11b0c442-989b-464c-8693-123456789051', 'OUT_WASTE', -0.5, '81b0c442-989b-464c-8693-123456789081', 'Điều chỉnh hao hụt kiểm kê', 250000, NOW(), 'system');
