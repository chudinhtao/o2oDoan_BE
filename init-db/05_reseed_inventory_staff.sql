-- SQL Script to Reseed Inventory and Staff Data (COMPLETE & 100% DATA DEPTH ACROSS ALL 15 TABLES)
-- Synchronized with Java Entities, pgvector, and PostgreSQL Schemas
-- Full historical depth, FEFO batches, real stock levels, POs, Stocktakes, and Transactions for all 25 ingredients

-- 1. CLEAN EXISTING DATA
TRUNCATE TABLE inventory.stock_transactions CASCADE;
TRUNCATE TABLE inventory.purchase_order_items CASCADE;
TRUNCATE TABLE inventory.purchase_orders CASCADE;
TRUNCATE TABLE inventory.stocktake_items CASCADE;
TRUNCATE TABLE inventory.stocktakes CASCADE;
TRUNCATE TABLE inventory.inventory_levels CASCADE;
TRUNCATE TABLE inventory.inventory_batches CASCADE;
TRUNCATE TABLE inventory.recipe_items CASCADE;
TRUNCATE TABLE inventory.recipes CASCADE;
TRUNCATE TABLE inventory.item_uom_conversions CASCADE;
TRUNCATE TABLE inventory.inventory_items CASCADE;
TRUNCATE TABLE inventory.item_categories CASCADE;
TRUNCATE TABLE inventory.suppliers CASCADE;
TRUNCATE TABLE inventory.locations CASCADE;
TRUNCATE TABLE inventory.uoms CASCADE;

TRUNCATE TABLE auth.attendance_logs CASCADE;
TRUNCATE TABLE auth.work_schedules CASCADE;
TRUNCATE TABLE auth.shift_templates CASCADE;

-- 2. SEED STAFF DATA (HRM)
INSERT INTO auth.shift_templates (id, name, start_time, end_time, color_code, active) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380001', 'Ca Sáng', '07:00:00', '12:00:00', '#60A5FA', TRUE),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380002', 'Ca Trưa (Peak)', '11:00:00', '14:00:00', '#F87171', TRUE),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380003', 'Ca Chiều', '12:00:00', '17:00:00', '#FBBF24', TRUE),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380004', 'Ca Tối', '17:00:00', '22:00:00', '#818CF8', TRUE),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380005', 'Full-time', '08:00:00', '17:00:00', '#34D399', TRUE);

DO $$
DECLARE
    v_admin_id UUID;
    v_cashier_id UUID;
    v_morning_shift_id UUID := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380001';
    v_peak_shift_id UUID := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380002';
    v_schedule_id UUID;
BEGIN
    SELECT id INTO v_admin_id FROM auth.users WHERE username = 'admin' LIMIT 1;
    SELECT id INTO v_cashier_id FROM auth.users WHERE username = 'cashier' LIMIT 1;

    IF v_admin_id IS NOT NULL THEN
        v_schedule_id := gen_random_uuid();
        INSERT INTO auth.work_schedules (id, user_id, shift_id, work_date, status, notes)
        VALUES (v_schedule_id, v_admin_id, v_morning_shift_id, CURRENT_DATE, 'COMPLETED', 'Làm việc nhiệt tình');
        
        INSERT INTO auth.attendance_logs (id, user_id, schedule_id, check_in, check_out, is_late, check_in_note)
        VALUES (gen_random_uuid(), v_admin_id, v_schedule_id, CURRENT_DATE + TIME '07:05:00', CURRENT_DATE + TIME '12:00:00', TRUE, 'Kẹt xe');
    END IF;

    IF v_cashier_id IS NOT NULL THEN
        INSERT INTO auth.work_schedules (id, user_id, shift_id, work_date, status)
        VALUES (gen_random_uuid(), v_cashier_id, v_peak_shift_id, CURRENT_DATE, 'PLANNED');
    END IF;
END $$;


-- 3. SEED INVENTORY MODULE

-- 3.1 UOMs (Units of Measure)
INSERT INTO inventory.uoms (id, name, short_name, created_at, created_by, updated_at, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'Kilogram', 'kg', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'Gram', 'g', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'Lít', 'l', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c04', 'Mililit', 'ml', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'Cái / Lon / Trái', 'cái', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c06', 'Thùng 24 lon', 'thùng', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', 'Bao 25kg', 'bao', NOW(), 'system', NOW(), 'system');

-- 3.2 Item Categories
INSERT INTO inventory.item_categories (id, name, created_at, created_by, updated_at, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', 'Nguyên liệu tươi', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', 'Gia vị & Đồ khô', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03', 'Đồ uống đóng chai', NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380a04', 'Vật tư tiêu hao', NOW(), 'system', NOW(), 'system');

-- 3.3 Suppliers
INSERT INTO inventory.suppliers (id, code, name, phone, email, address, tax_code, is_active, created_at, created_by, updated_at, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'DALAT', 'Nông sản Sạch Đà Lạt', '0901234567', 'lan@dalatfarm.vn', 'Đức Trọng, Lâm Đồng', '5801234567', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'METRO', 'Thực phẩm Metro Mega', '0918887766', 'sales@metro.com.vn', 'An Phú, Quận 2, TP.HCM', '0301112223', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'THP', 'Tập đoàn Tân Hiệp Phát', '028333444', 'contact@thp.com.vn', 'Thuận An, Bình Dương', '3700123456', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b04', 'CP', 'Tập đoàn Chăn nuôi C.P. Việt Nam', '02513836251', 'contact@cp.com.vn', 'KCN Biên Hòa 2, Đồng Nai', '3600123457', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b05', 'BH-SUGAR', 'Công ty Cổ phần Đường Biên Hòa', '02513836181', 'sales@bienhoasugar.com.vn', 'KCN Biên Hòa 1, Đồng Nai', '3600123458', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'VNM', 'Công ty Cổ phần Sữa Việt Nam (Vinamilk)', '02854155555', 'vinamilk@vinamilk.com.vn', 'Tân Phú, Quận 7, TP.HCM', '0300588569', TRUE, NOW(), 'system', NOW(), 'system');

-- 3.4 Locations (Warehouses)
INSERT INTO inventory.locations (id, name, is_active, created_at, created_by, updated_at, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', 'Kho Tổng', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', 'Kho Bếp chính', TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', 'Tủ mát Quầy Bar', TRUE, NOW(), 'system', NOW(), 'system');

-- 3.5 Inventory Items (Raw materials / Ingredients - 25 Premium items)
INSERT INTO inventory.inventory_items (id, name, sku, category_id, base_uom_id, type, avg_cost_price, safety_stock, is_active, created_at, created_by, updated_at, updated_by) VALUES
-- Meat & Seafood (Nguyên liệu tươi)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 'Thịt Bò Mỹ', 'BEEF-US-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 250000.00, 10.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 'Gà Miếng Phi Lê', 'CHICKEN-FILLET', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 120000.00, 15.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 'Sườn Heo Cánh Buồm', 'PORK-RIB-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 150000.00, 10.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', 'Tôm Sú Côn Đảo', 'SHRIMP-CD-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 320000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e10', 'Cua Đồng Xay Sẵn', 'CRAB-MINCED', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 90000.00, 8.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', 'Mực Ống Tươi Cấp Đông', 'SQUID-FROZEN', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 220000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),

-- Fresh organic farm products
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 'Rau Củ Quả Sạch Đà Lạt', 'VEG-ORGANIC', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 25000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e22', 'Cam Sành Tươi Vắt Nước', 'ORANGE-FRESH', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 30000.00, 15.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', 'Xoài Cát Chu Chín Ngọt', 'MANGO-FRESH', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 40000.00, 10.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e24', 'Dừa Xiêm Bến Tre', 'COCONUT-WHOLE', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'RAW', 15000.00, 30.0, TRUE, NOW(), 'system', NOW(), 'system'),

-- Starches & Dry Ingredients (Gia vị & Đồ khô)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', 'Gạo Thơm ST25', 'RICE-ST25', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 35000.00, 50.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e06', 'Khoai Tây Bỉ Cắt Lát', 'POTATO-SLICE', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 60000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e07', 'Bột Chiên Xù Panko', 'PANKO-POWDER', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'RAW', 15.00, 5000.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e12', 'Bánh Phở Tươi Hà Nội', 'RICE-NOODLE-PHO', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 22000.00, 30.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', 'Sợi Bún Tươi Loại 1', 'RICE-NOODLE-BUN', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 18000.00, 30.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e14', 'Cùi Bưởi Đường Sấy', 'POMELO-PEEL', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 120000.00, 10.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e04', 'Dầu ăn Tường An 1L', 'OIL-TA-1L', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'RAW', 45000.00, 12.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e18', 'Gia Vị Nhà Hàng Tổng Hợp', 'SPICE-MIX', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 50000.00, 15.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e19', 'Trà Khô Thái Nguyên loại 1', 'TEA-THAI-NGUYEN', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 180000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 'Sữa Đặc & Nước Cốt Dừa Hộp', 'MILK-COCONUT-MIX', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'RAW', 60000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e25', 'Bột Làm Bánh Flan & Kem', 'FLAN-POWDER', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 80000.00, 10.0, TRUE, NOW(), 'system', NOW(), 'system'),

-- Beverages & Retail (Đồ uống & Đóng chai)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e03', 'Coca Cola Lon 330ml', 'COCA-330', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'RETAIL', 8000.00, 120.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e15', 'Nước Mía Cây Ép Sẵn', 'SUGARCANE-JUICE', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'RAW', 25000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e21', 'Bia Sài Gòn Chill Lon', 'BEER-SAIGON-CHILL', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'RETAIL', 15000.00, 48.0, TRUE, NOW(), 'system', NOW(), 'system'),

-- Newly Added Realistic Ingredients for Modifiers & Realistic Recipes
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', 'Trứng Gà Tươi', 'EGG-FRESH-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'RAW', 3500.00, 100.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', 'Ớt Chỉ Thiên Tươi', 'CHILI-FRESH-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 45000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', 'Đường Cát Trắng Biên Hòa', 'SUGAR-WHITE-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 22000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e29', 'Đá Viên Tinh Khiết', 'ICE-PURE-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'CONSUMABLE', 1500.00, 100.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e30', 'Trà Xanh Matcha Uji Nhật Bản', 'MATCHA-POWDER', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 'RAW', 1500.00, 1000.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e31', 'Sầu Riêng Ri6 Đóng Hộp', 'DURIAN-RI6', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 180000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e32', 'Bột Mỳ Đa Dụng Meizan', 'FLOUR-MEIZAN', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 25000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e33', 'Mật Ong Nhãn Tự Nhiên', 'HONEY-NATURAL', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 'RAW', 300000.00, 5.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e34', 'Bột Nếp Thái Loại Đặc Biệt', 'GLUTIN-FLOUR', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 'RAW', 32000.00, 20.0, TRUE, NOW(), 'system', NOW(), 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e35', 'Hộp Đựng Thực Phẩm Nhựa tròn', 'PLASTIC-BOX-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'CONSUMABLE', 1500.00, 500.0, TRUE, NOW(), 'system', NOW(), 'system'),

-- Consumables (Vật tư tiêu hao)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16', 'Hộp Đựng Thực Phẩm Giấy', 'PAPER-BOX-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 'CONSUMABLE', 2000.00, 500.0, TRUE, NOW(), 'system', NOW(), 'system');

-- 3.6 Item UOM Conversions
INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by, updated_at, updated_by) VALUES
-- Drinks conversions (Thùng -> Lon)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 24.00, NOW(), 'system', NOW(), 'system'), -- Coca Cola: 1 Thùng = 24 Lon
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e21', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 24.00, NOW(), 'system', NOW(), 'system'), -- Bia Sài Gòn: 1 Thùng = 24 Lon

-- Starches & Dry stocks conversions (Bao -> Kilogram)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Gạo Thơm ST25: 1 Bao = 25 kg
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e32', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Bột Mỳ Meizan: 1 Bao = 25 kg
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e34', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Bột Nếp Thái: 1 Bao = 25 kg
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Đường Biên Hòa: 1 Bao = 25 kg

-- Meat conversions (Bulk Box/Bao -> Kilogram)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Thịt Bò Mỹ: 1 Hộp Sỉ = 25 kg
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'), -- Gà Miếng Phi Lê: 1 Hộp Sỉ = 25 kg
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38cc09', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 25.00, NOW(), 'system', NOW(), 'system'); -- Sườn Heo: 1 Hộp Sỉ = 25 kg -- 1 Bao = 25 kg

-- 3.7 Inventory Batches (FEFO Traceability - Seeded for all fresh batch-controlled items)
INSERT INTO inventory.inventory_batches (id, item_id, lot_number, manufacture_date, expiry_date, is_active, created_at, updated_at, created_by, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 'LOT-BEEF-001', CURRENT_DATE - 3, CURRENT_DATE + 4, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 'LOT-CHICKEN-001', CURRENT_DATE - 2, CURRENT_DATE + 5, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 'LOT-PORK-001', CURRENT_DATE - 2, CURRENT_DATE + 3, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', 'LOT-SHRIMP-001', CURRENT_DATE - 1, CURRENT_DATE + 2, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e12', 'LOT-NOODLE-001', CURRENT_DATE, CURRENT_DATE + 1, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', 'LOT-SQUID-001', CURRENT_DATE - 2, CURRENT_DATE + 3, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e22', 'LOT-ORANGE-001', CURRENT_DATE, CURRENT_DATE + 3, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', 'LOT-MANGO-001', CURRENT_DATE - 1, CURRENT_DATE + 2, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba09', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', 'LOT-EGG-001', CURRENT_DATE - 1, CURRENT_DATE + 10, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba10', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', 'LOT-CHILI-001', CURRENT_DATE - 2, CURRENT_DATE + 6, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba11', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e31', 'LOT-DURIAN-001', CURRENT_DATE - 5, CURRENT_DATE + 15, TRUE, NOW(), NOW(), 'system', 'system'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba12', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e33', 'LOT-HONEY-001', CURRENT_DATE - 10, CURRENT_DATE + 365, TRUE, NOW(), NOW(), 'system', 'system');

-- 3.8 Inventory Levels (HIGH DEPTH - REALISTIC STOCK LEVELS SEEDED FOR ALL 25 PREMIUM INGREDIENTS)
INSERT INTO inventory.inventory_levels (id, item_id, location_id, batch_id, current_stock, allocated_stock, last_modified_by, updated_at, created_at, created_by, updated_by) VALUES
-- Meat & Seafood in Kho Tổng (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01)
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba01', 50.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba02', 80.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba03', 45.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba04', 30.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f14', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e10', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 25.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f15', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba06', 40.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),

-- Fresh Farm Products in Kho Bếp (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f09', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 120.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f10', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e22', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba07', 50.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f11', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba08', 40.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f16', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e24', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 60.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),

-- Starches & Dry stocks in Kho Bếp (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 150.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 30.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e12', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba05', 20.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f17', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 40.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f18', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e07', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 25000.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f19', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 35.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f20', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e14', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 15.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f12', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e18', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 25.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f21', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e19', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 8.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f22', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 30.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f23', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e25', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 12.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),

-- Drinks in Quầy Bar (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03)
('300ebc99-9c0b-4ef8-bb6d-6bb9bd380f08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', NULL, 120.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('300ebc99-9c0b-4ef8-bb6d-6bb9bd380f24', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e15', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', NULL, 30.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('300ebc99-9c0b-4ef8-bb6d-6bb9bd380f13', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e21', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', NULL, 96.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('300ebc99-9c0b-4ef8-bb6d-6bb9bd380f90', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e29', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', NULL, 200.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Đá Viên (Quầy Bar)
('300ebc99-9c0b-4ef8-bb6d-6bb9bd380f91', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d03', NULL, 10.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Đường (Quầy Bar)

-- Consumables in Kho Tổng
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f25', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 650.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'),
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f85', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e35', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 800.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Hộp nhựa (Kho Tổng)

-- New Levels in Kho Tổng
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f92', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 500.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Trứng Gà (Kho Tổng)
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f93', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 10.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Ớt Chỉ Thiên (Kho Tổng)
('100ebc99-9c0b-4ef8-bb6d-6bb9bd380f94', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 100.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Đường Cát (Kho Tổng)

-- New Levels in Kho Bếp
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f95', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 150.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Trứng Gà (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f96', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 3.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Ớt Chỉ Thiên (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f97', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 20.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Đường Cát (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f80', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e30', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 5000.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Matcha (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f81', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e31', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 15.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Sầu Riêng (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f82', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e32', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 40.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Bột Mỳ (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f83', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e33', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 10.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'), -- Mật Ong (Kho Bếp)
('200ebc99-9c0b-4ef8-bb6d-6bb9bd380f84', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e34', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 30.0, 0.0, NULL, NOW(), NOW(), 'system', 'system'); -- Bột Nếp (Kho Bếp)

-- 3.9 Recipes (COMPREHENSIVE - EXACTLY 40 RECIPES CREATED FOR 40 MENU ITEMS)
INSERT INTO inventory.recipes (id, sale_item_id, modifier_id, type, created_at, created_by, updated_at, updated_by) VALUES
-- Category 1: Khai vị (Appetizers - 8 items)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380701', '59be1713-b9c8-495f-8689-cc3bab94f225', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Gà Rán Phần
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380702', '9a1658a2-90a8-431e-bf47-3ad91eaaf9c1', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Khoai Tây Chiên
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380711', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f002', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bánh Mì Muối Ớt
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380712', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f005', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Gỏi Cuốn Tôm Thịt
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380713', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f001', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Nem Rán Hà Nội
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380714', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f004', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Ngô Chiên Bơ Tỏi
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380715', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f003', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Salad Ức Gà Áp Chảo
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380716', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f006', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Súp Măng Tây Cua

-- Category 2: Món Nước, Cơm & Bún (Noodles, Rice, Bun - 8 items)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380703', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f101', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Phở Bò Tái Lăn
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380704', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f105', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Cơm Tấm Sườn Bì Chả
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380705', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f104', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bún Chả Hà Nội
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380721', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f106', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bún Bò Huế Đặc Biệt
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380722', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f108', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Cơm Gà Hải Nam Dẻo Thơm
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380723', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f103', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Cơm Rang Dưa Bò
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380724', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f107', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Mỳ Quảng Gà Trứng
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380725', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f102', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Phở Gà Ta Cổ Điển

-- Category 3: Món Ăn Chính, Lẩu & Nướng (Main, Hotpot, Grilled - 8 items)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380706', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f201', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Lẩu Thái Hải Sản (Nhỏ)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380731', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f203', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bò Nướng Tảng Sốt Tiêu
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380732', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f206', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Cá Quả Nướng Riềng Sả M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380733', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f205', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Gà Nướng Mắc Khén Tây Bắc
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380734', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f207', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Lẩu Nấm Chay Thanh Đạm M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380735', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f202', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Lẩu Riêu Cua Sườn Sụn (Nhỏ)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380736', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f208', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Mực Trứng Nướng Sa Tế M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380737', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f204', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Sườn Nướng BBQ Tảng M

-- Category 4: Tráng Miệng (Desserts - 8 items)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380707', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f301', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Chè Bưởi An Giang
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380741', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f303', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bánh Flan Trà Xanh M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380742', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f308', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bánh Su Kem Mini
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380743', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f302', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Chè Thái Sầu Riêng M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380744', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f307', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Hoa Quả Tươi Theo Mùa M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380745', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f305', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Kem Dừa Côn Đảo
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380746', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f306', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Rau Câu Dừa Xiêm M
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380747', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f304', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Sữa Yogurt Nếp Cẩm

-- Category 5: Đồ Uống Giải Khát (Beverages - 8 items)
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380708', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f402', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Nước Mía Siêu Sạch
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380751', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f407', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Bia Sài Gòn Chill Lạnh
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380752', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f406', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Coca Cola Lon
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380753', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f408', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Nước Cam Vắt Tự Nhiên
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380754', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f405', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Nước Dừa Xiêm Tươi
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380755', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f404', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Sinh Tố Xoài Cát Chu
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380756', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f401', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'), -- Trà Đá Mát Lạnh
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380757', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f403', NULL, 'MAIN_ITEM', NOW(), 'system', NOW(), 'system'); -- Trà Tắc Mật Ong Vàng


-- 3.10 Recipe Items (PROPORTIONS MAPPED WITH 100% OPERATIONAL LOGIC & CHECK CONSTRAINT SAFE)
INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by) VALUES
-- 1. Gà Rán Phần
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380701', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.20, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380701', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e07', 30.0, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 10.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380701', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e04', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 2. Khoai Tây Chiên
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380702', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e06', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380702', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e04', 0.03, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 3. Bánh Mì Muối Ớt
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380711', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380711', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e18', 0.01, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 4. Gỏi Cuốn Tôm Thịt
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380712', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380712', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 5. Nem Rán Hà Nội
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380713', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.10, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380713', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 6. Ngô Chiên Bơ Tỏi
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380714', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 7. Salad Ức Gà Áp Chảo
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380715', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380715', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 8. Súp Măng Tây Cua
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380716', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e10', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 1.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 9. Phở Bò Tái Lăn
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380703', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e12', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380703', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 0.08, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 4.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 10. Cơm Tấm Sườn Bì Chả
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380704', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', 0.12, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380704', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 0.20, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 8.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 11. Bún Chả Hà Nội
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380705', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380705', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 12. Bún Bò Huế Đặc Biệt
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380721', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 0.08, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 4.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380721', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 13. Cơm Gà Hải Nam Dẻo Thơm
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380722', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.20, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380722', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', 0.12, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 14. Cơm Rang Dưa Bò
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380723', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', 0.12, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380723', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 0.08, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 4.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 15. Mỳ Quảng Gà Trứng
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380724', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.10, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380724', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e13', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 16. Phở Gà Ta Cổ Điển
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380725', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e12', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380725', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.10, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 17. Lẩu Thái Hải Sản (Nhỏ)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380706', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380706', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', 0.15, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 18. Bò Nướng Tảng Sốt Tiêu
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380731', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 0.25, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380731', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e18', 0.02, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 19. Cá Quả Nướng Riềng Sả M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380732', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', 0.30, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 20. Gà Nướng Mắc Khén Tây Bắc
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380733', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 0.35, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 21. Lẩu Nấm Chay Thanh Đạm M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380734', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.40, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 22. Lẩu Riêu Cua Sườn Sụn (Nhỏ)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380735', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e10', 0.25, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380735', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 0.20, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 3.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 23. Mực Trứng Nướng Sa Tế M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380736', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', 0.25, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 24. Sườn Nướng BBQ Tảng M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380737', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 0.35, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 25. Chè Bưởi An Giang
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380707', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e14', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380707', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 26. Bánh Flan Trà Xanh M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380741', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e25', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380741', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380741', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e30', 5.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 5g Trà Xanh Matcha Uji

-- 27. Bánh Su Kem Mini
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380742', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e32', 0.03, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 30g Bột mỳ Meizan
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380742', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', 0.50, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 0.5 quả Trứng gà tươi
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380742', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e25', 0.02, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 2.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 20g bột flan làm kem custard

-- 28. Chè Thái Sầu Riêng M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380743', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e31', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 50g Sầu Riêng Ri6 Đóng Hộp
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380743', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380743', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.08, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 29. Hoa Quả Tươi Theo Mùa M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380744', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.25, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 30. Kem Dừa Côn Đảo
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380745', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e24', 0.50, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380745', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 31. Rau Câu Dừa Xiêm M
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380746', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e24', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 32. Sữa Yogurt Nếp Cẩm
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380747', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e34', 0.03, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'), -- 30g Bột Nếp Thái
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380747', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.05, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 33. Nước Mía Siêu Sạch
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380708', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e15', 0.30, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 1.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 34. Bia Sài Gòn Chill Lạnh
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380751', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e21', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 35. Coca Cola Lon
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380752', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e03', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 36. Nước Cam Vắt Tự Nhiên
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380753', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e22', 0.30, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 37. Nước Dừa Xiêm Tươi
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380754', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e24', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 38. Sinh Tố Xoài Cát Chu
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380755', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', 0.20, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380755', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', 0.03, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 39. Trà Đá Mát Lạnh
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380756', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e19', 0.005, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),

-- 40. Trà Tắc Mật Ong Vàng
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380757', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e19', 0.005, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380757', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', 0.02, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380757', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e33', 0.015, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');


-- 3.11 Purchase Orders (PO - Fully detailed orders with realistic, expanded ingredients)
INSERT INTO inventory.purchase_orders (id, po_number, supplier_id, status, type, total_amount, expected_date, notes, created_at, created_by, updated_at, updated_by, confirmed_at) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', 'PO-20260515-01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b02', 'COMPLETED', 'STANDARD', 18500000.00, CURRENT_DATE - 3, 'Đơn nhập hàng Metro số lượng lớn đầu tuần', NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092', 'PO-20260518-02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b01', 'CONFIRMED', 'STANDARD', 6800000.00, CURRENT_DATE + 2, 'Nhập bổ sung rau củ quả & hoa quả tươi Đà Lạt', NOW(), 'admin', NOW(), 'admin', NOW()),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a093', 'PO-20260518-03', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b03', 'DRAFT', 'STANDARD', 3600000.00, CURRENT_DATE + 4, 'Đơn nháp nhập Coca lon cho quầy bar', NOW(), 'admin', NOW(), 'admin', NULL),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', 'PO-20260516-04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b04', 'COMPLETED', 'STANDARD', 27500000.00, CURRENT_DATE - 2, 'Nhập sỉ trứng gà sạch và thịt tươi từ CP Group', NOW() - INTERVAL '2 days', 'admin', NOW() - INTERVAL '2 days', 'admin', NOW() - INTERVAL '2 days'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a095', 'PO-20260517-05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b05', 'COMPLETED', 'STANDARD', 10000000.00, CURRENT_DATE - 1, 'Đơn nhập đường cát số lượng lớn chuẩn bị mùa nắng nóng', NOW() - INTERVAL '1 day', 'admin', NOW() - INTERVAL '1 day', 'admin', NOW() - INTERVAL '1 day'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a096', 'PO-20260518-06', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380b06', 'CONFIRMED', 'STANDARD', 11000000.00, CURRENT_DATE + 3, 'Đơn sữa đặc dự kiến giao tuần sau từ Vinamilk', NOW(), 'admin', NOW(), 'admin', NOW());

-- 3.12 Purchase Order Items (HIGH DEPTH - MAPS TO 10 DIFFERENT INGREDIENTS WITH PERFECT NOT-NULL COMPLIANCE)
INSERT INTO inventory.purchase_order_items (id, po_id, item_id, uom_id, quantity, ordered_quantity, received_quantity, unit_price, batch_number, expiry_date, created_at, created_by, updated_at, updated_by) VALUES
-- Items for completed PO-01
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 50.0, 50.0, 50.0, 250000.00, 'LOT-BEEF-001', CURRENT_DATE + 4, NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 80.0, 80.0, 80.0, 120000.00, 'LOT-CHICKEN-001', CURRENT_DATE + 5, NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 45.0, 45.0, 45.0, 150000.00, 'LOT-PORK-001', CURRENT_DATE + 3, NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 150.0, 150.0, 150.0, 35000.00, NULL, NULL, NOW() - INTERVAL '3 days', 'admin', NOW() - INTERVAL '3 days', 'admin'),

-- Items for confirmed PO-02 (Not received yet)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e09', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 10.0, 10.0, 0.0, 320000.00, 'LOT-SHRIMP-002', CURRENT_DATE + 3, NOW(), 'admin', NOW(), 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e11', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 10.0, 10.0, 0.0, 220000.00, 'LOT-SQUID-002', CURRENT_DATE + 4, NOW(), 'admin', NOW(), 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e17', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 120.0, 120.0, 0.0, 25000.00, NULL, NULL, NOW(), 'admin', NOW(), 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e23', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 40.0, 40.0, 0.0, 40000.00, 'LOT-MANGO-002', CURRENT_DATE + 2, NOW(), 'admin', NOW(), 'admin'),

-- Items for completed PO-04 (CP Group)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 1000.0, 1000.0, 1000.0, 3000.00, 'LOT-EGG-002', CURRENT_DATE + 8, NOW() - INTERVAL '2 days', 'admin', NOW() - INTERVAL '2 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 100.0, 100.0, 100.0, 120000.00, 'LOT-CHICKEN-002', CURRENT_DATE + 4, NOW() - INTERVAL '2 days', 'admin', NOW() - INTERVAL '2 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 50.0, 50.0, 50.0, 250000.00, 'LOT-BEEF-002', CURRENT_DATE + 3, NOW() - INTERVAL '2 days', 'admin', NOW() - INTERVAL '2 days', 'admin'),

-- Items for completed PO-05 (Bien Hoa Sugar)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a095', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 500.0, 500.0, 500.0, 20000.00, 'LOT-SUGAR-002', CURRENT_DATE + 180, NOW() - INTERVAL '1 day', 'admin', NOW() - INTERVAL '1 day', 'admin'),

-- Items for confirmed PO-06 (Vinamilk - Not received yet)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a096', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e20', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 200.0, 200.0, 0.0, 55000.00, 'LOT-MILK-002', CURRENT_DATE + 30, NOW(), 'admin', NOW(), 'admin');

-- 3.13 Stocktakes
INSERT INTO inventory.stocktakes (id, name, status, snapshot_time, completed_at, notes, created_at, created_by, updated_at, updated_by) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', 'Kiểm kê kho lạnh định kỳ đầu ca sáng', 'COMPLETED', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '11 hours', 'Tất cả nguyên liệu tươi đều ở trạng thái bảo quản tốt.', NOW() - INTERVAL '12 hours', 'admin', NOW() - INTERVAL '11 hours', 'admin'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38d092', 'Kiểm kho đồ khô cuối tuần', 'DRAFT', NOW(), NULL, 'Đang tiến hành đếm số bao gạo và chai dầu ăn.', NOW(), 'admin', NOW(), 'admin');

-- 3.14 Stocktake Items (Enriched with 4 typical ingredients for higher depth)
INSERT INTO inventory.stocktake_items (id, stocktake_id, item_id, system_quantity, counted_quantity, variance, adjustment_reason, created_at, created_by, updated_at, updated_by) VALUES
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 50.0, 49.8, -0.2, 'Hao hụt rã đông & cắt thái góc thịt', NOW() - INTERVAL '12 hours', 'admin', NOW() - INTERVAL '11 hours', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', 80.0, 80.0, 0.0, 'Đầy đủ, chuẩn số lượng', NOW() - INTERVAL '12 hours', 'admin', NOW() - INTERVAL '11 hours', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 45.0, 44.5, -0.5, 'Sườn lọc bỏ bớt mỡ rìa ngoài', NOW() - INTERVAL '12 hours', 'admin', NOW() - INTERVAL '11 hours', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', 150.0, 149.0, -1.0, 'Hao hụt khi chia túi nhỏ bảo quản', NOW() - INTERVAL '12 hours', 'admin', NOW() - INTERVAL '11 hours', 'admin');

-- 3.15 Stock Transactions (REALISTIC FLOW HISTORY LINKED TO THE COMPLETED PO AND STOCKTAKES)
INSERT INTO inventory.stock_transactions (id, item_id, location_id, batch_id, transaction_type, quantity_change, unit_price_at_transaction, reference_id, order_line_item_id, reason, created_at, created_by) VALUES
-- PO Inbound transactions for PO-01 (Beef, Chicken, Pork, Rice)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba01', 'IN_PO', 50.0, 250000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', NULL, 'Nhập hàng từ đơn PO-20260515-01', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba02', 'IN_PO', 80.0, 120000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', NULL, 'Nhập hàng từ đơn PO-20260515-01', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba03', 'IN_PO', 45.0, 150000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', NULL, 'Nhập hàng từ đơn PO-20260515-01', NOW() - INTERVAL '3 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 'IN_PO', 150.0, 35000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091', NULL, 'Nhập hàng từ đơn PO-20260515-01', NOW() - INTERVAL '3 days', 'admin'),

-- Waste transactions from morning stocktake (Beef, Pork, Rice loss)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba01', 'OUT_WASTE', -0.2, 250000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', NULL, 'Hao hụt kiểm kê đầu ca sáng', NOW() - INTERVAL '11 hours', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba03', 'OUT_WASTE', -0.5, 150000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', NULL, 'Hao hụt lọc mỡ sườn', NOW() - INTERVAL '11 hours', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', NULL, 'OUT_WASTE', -1.0, 35000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38d091', NULL, 'Hao hụt chia túi nhỏ gạo', NOW() - INTERVAL '11 hours', 'admin'),

-- PO Inbound transactions for PO-04 (CP Group: Eggs, Chicken, Beef)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba09', 'IN_PO', 1000.0, 3000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', NULL, 'Nhập sỉ trứng gà sạch từ đơn PO-20260516-04', NOW() - INTERVAL '2 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e05', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba02', 'IN_PO', 100.0, 120000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', NULL, 'Nhập thịt gà tươi ta thả vườn từ đơn PO-20260516-04', NOW() - INTERVAL '2 days', 'admin'),
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38ba01', 'IN_PO', 50.0, 250000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094', NULL, 'Nhập thịt bò Mỹ cao cấp từ đơn PO-20260516-04', NOW() - INTERVAL '2 days', 'admin'),

-- PO Inbound transactions for PO-05 (Bien Hoa Sugar: Sugar)
(gen_random_uuid(), '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', NULL, 'IN_PO', 500.0, 20000.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a095', NULL, 'Nhập đường cát Biên Hòa từ đơn PO-20260517-05', NOW() - INTERVAL '1 day', 'admin');


-- 3.16 DYNAMIC MODIFIER RECIPE SEEDING FOR ALL ITEM OPTIONS (120+ MODIFIERS SUPPORTED IDEMPOTENTLY)
DO $$
DECLARE
    r_opt RECORD;
    v_rec_id UUID;
BEGIN
    FOR r_opt IN SELECT id, name FROM menu.item_options LOOP
        -- check if a recipe already exists for this modifier_id
        IF NOT EXISTS (SELECT 1 FROM inventory.recipes WHERE modifier_id = r_opt.id) THEN
            v_rec_id := gen_random_uuid();
            
            -- Insert the parent recipe
            INSERT INTO inventory.recipes (id, sale_item_id, modifier_id, type, created_at, created_by, updated_at, updated_by)
            VALUES (v_rec_id, NULL, r_opt.id, 'MODIFIER', NOW(), 'system', NOW(), 'system');
            
            -- If it is an extra ingredient option, let's link its recipe items!
            IF r_opt.name LIKE '%Thêm Thịt Bò%' THEN
                -- Thêm 50g Thịt Bò Mỹ
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e01', 0.0500, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Thêm Chả Giò Chiên%' OR r_opt.name LIKE '%Thêm Chả%' THEN
                -- Thêm 50g Sườn Heo (làm chả giò)
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e08', 0.0500, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 5.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Thêm Trứng%' THEN
                -- Thêm 1 quả Trứng Gà Tươi thực tế!
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e26', 1.0000, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Lớn%' OR r_opt.name LIKE '%L' THEN
                -- Size Lớn: tiêu tốn thêm 1 Hộp Đựng Thực Phẩm Giấy (Chỉ khi mang về - TAKEAWAY_ONLY)
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16', 1.0000, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'TAKEAWAY_ONLY', NOW(), 'system', NOW(), 'system');
                -- Và thêm 10g Gia Vị Tổng Hợp cho phần sốt lớn hơn (ALWAYS)
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e18', 0.0100, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Cay Vừa%' THEN
                -- Cay Vừa: thêm 5g Ớt Chỉ Thiên Tươi thực tế!
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', 0.0050, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Cay Nhiều%' THEN
                -- Cay Nhiều: thêm 15g Ớt Chỉ Thiên Tươi thực tế!
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e27', 0.0150, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Đá & Đường Bình Thường%' THEN
                -- Bình thường: tiêu hao 50g Đá viên & 10g Đường cát
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e29', 0.0500, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', 0.0100, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                
            ELSIF r_opt.name LIKE '%Ít Đá%' OR r_opt.name LIKE '%Ít Đường%' THEN
                -- Ít Đá / Ít Đường: chỉ tiêu hao 25g Đá viên & 5g Đường cát
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e29', 0.0250, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
                INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_rec_id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e28', 0.0050, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.00, 'ALWAYS', NOW(), 'system', NOW(), 'system');
            END IF;
        END IF;
    END LOOP;
END $$;


-- 3.17 DYNAMICALLY ADD TAKEAWAY PACKAGING FOR ALL MAIN MENU ITEMS (TAKEAWAY_ONLY SCOPE)
DO $$
DECLARE
    r_recipe RECORD;
BEGIN
    FOR r_recipe IN SELECT id FROM inventory.recipes WHERE type = 'MAIN_ITEM' LOOP
        -- check if a takeaway paper box already exists in this recipe
        IF NOT EXISTS (SELECT 1 FROM inventory.recipe_items WHERE recipe_id = r_recipe.id AND inventory_item_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16' AND scope = 'TAKEAWAY_ONLY') THEN
            INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, scope, created_at, created_by, updated_at, updated_by)
            VALUES (gen_random_uuid(), r_recipe.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e16', 1.00, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c05', 0.00, 'TAKEAWAY_ONLY', NOW(), 'system', NOW(), 'system');
        END IF;
    END LOOP;
END $$;


-- 3.18 DYNAMIC AUDIT & AUTO-SUPPLEMENT FOR SUPPLIER LINKAGE AND DUAL BATCH COMPLIANCE
DO $$
DECLARE
    r_item RECORD;
    v_po_id UUID;
    v_count INT;
    v_lot1 UUID;
    v_lot2 UUID;
BEGIN
    FOR r_item IN SELECT id, sku, name, category_id, base_uom_id, avg_cost_price FROM inventory.inventory_items LOOP
        
        -- 1. Ensure at least 2 Purchase Order Items exist for this ingredient (linking it to a Supplier)
        SELECT COUNT(*) INTO v_count FROM inventory.purchase_order_items WHERE item_id = r_item.id;
        
        IF v_count < 2 THEN
            -- Determine the best PO/Supplier based on category
            IF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01' THEN
                -- Link Fresh items to Dalat (PO-02) or CP (PO-04)
                v_po_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a094'; -- CP Group
            ELSIF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03' THEN
                v_po_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a093'; -- THP
            ELSE
                v_po_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091'; -- Metro
            END IF;

            -- Add first PO item if none exist
            IF v_count = 0 THEN
                INSERT INTO inventory.purchase_order_items (id, po_id, item_id, uom_id, quantity, ordered_quantity, received_quantity, unit_price, batch_number, expiry_date, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), v_po_id, r_item.id, r_item.base_uom_id, 100.0, 100.0, 100.0, COALESCE(r_item.avg_cost_price, 10000.00), 'LOT-' || r_item.sku || '-001', CURRENT_DATE + 30, NOW() - INTERVAL '2 days', 'system', NOW() - INTERVAL '2 days', 'system');
            END IF;

            -- Add second PO item to ensure at least 2 distinct PO purchases (two suppliers/orders)
            IF v_po_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091' THEN
                v_po_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a092'; -- Dalat as alternative
            ELSE
                v_po_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a091'; -- Metro as default alternative
            END IF;

            INSERT INTO inventory.purchase_order_items (id, po_id, item_id, uom_id, quantity, ordered_quantity, received_quantity, unit_price, batch_number, expiry_date, created_at, created_by, updated_at, updated_by)
            VALUES (gen_random_uuid(), v_po_id, r_item.id, r_item.base_uom_id, 50.0, 50.0, 50.0, COALESCE(r_item.avg_cost_price, 10000.00), 'LOT-' || r_item.sku || '-002', CURRENT_DATE + 45, NOW() - INTERVAL '1 day', 'system', NOW() - INTERVAL '1 day', 'system');
        END IF;

        -- 2. Ensure at least 2 Batches exist for this ingredient
        SELECT COUNT(*) INTO v_count FROM inventory.inventory_batches WHERE item_id = r_item.id;
        
        IF v_count < 2 THEN
            -- If no batches exist at all
            IF v_count = 0 THEN
                v_lot1 := gen_random_uuid();
                INSERT INTO inventory.inventory_batches (id, item_id, lot_number, manufacture_date, expiry_date, is_active, created_at, updated_at, created_by, updated_by)
                VALUES (v_lot1, r_item.id, 'LOT-' || r_item.sku || '-001', CURRENT_DATE - 5, CURRENT_DATE + 30, TRUE, NOW(), NOW(), 'system', 'system');
            ELSE
                SELECT id INTO v_lot1 FROM inventory.inventory_batches WHERE item_id = r_item.id LIMIT 1;
            END IF;

            -- Add second batch to ensure at least 2 distinct batches
            v_lot2 := gen_random_uuid();
            INSERT INTO inventory.inventory_batches (id, item_id, lot_number, manufacture_date, expiry_date, is_active, created_at, updated_at, created_by, updated_by)
            VALUES (v_lot2, r_item.id, 'LOT-' || r_item.sku || '-002', CURRENT_DATE - 3, CURRENT_DATE + 45, TRUE, NOW(), NOW(), 'system', 'system');
        END IF;

        -- 3. Ensure at least 2 Inventory Levels exist (Kho Tổng and Kho Bếp)
        SELECT COUNT(*) INTO v_count FROM inventory.inventory_levels WHERE item_id = r_item.id;
        
        IF v_count < 2 THEN
            -- Get or create batch IDs
            SELECT id INTO v_lot1 FROM inventory.inventory_batches WHERE item_id = r_item.id AND lot_number LIKE '%-001' LIMIT 1;
            SELECT id INTO v_lot2 FROM inventory.inventory_batches WHERE item_id = r_item.id AND lot_number LIKE '%-002' LIMIT 1;
            
            IF v_lot1 IS NULL THEN
                SELECT id INTO v_lot1 FROM inventory.inventory_batches WHERE item_id = r_item.id LIMIT 1;
            END IF;
            IF v_lot2 IS NULL THEN
                SELECT id INTO v_lot2 FROM inventory.inventory_batches WHERE item_id = r_item.id ORDER BY created_at DESC LIMIT 1;
            END IF;

            -- Seed level in Kho Tổng (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01) if not exists
            IF NOT EXISTS (SELECT 1 FROM inventory.inventory_levels WHERE item_id = r_item.id AND location_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01') THEN
                INSERT INTO inventory.inventory_levels (id, item_id, location_id, batch_id, current_stock, allocated_stock, last_modified_by, updated_at, created_at, created_by, updated_by)
                VALUES (gen_random_uuid(), r_item.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d01', v_lot1, 100.0, 0.0, NULL, NOW(), NOW(), 'system', 'system');
            END IF;

            -- Seed level in Kho Bếp chính (000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02) if not exists
            IF NOT EXISTS (SELECT 1 FROM inventory.inventory_levels WHERE item_id = r_item.id AND location_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02') THEN
                INSERT INTO inventory.inventory_levels (id, item_id, location_id, batch_id, current_stock, allocated_stock, last_modified_by, updated_at, created_at, created_by, updated_by)
                VALUES (gen_random_uuid(), r_item.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380d02', v_lot2, 50.0, 0.0, NULL, NOW(), NOW(), 'system', 'system');
            END IF;
        END IF;

        -- 4. Auto-register standard metric conversions (g -> kg, ml -> l, kg -> g)
        -- If base UOM is Kilogram (kg), add conversion from Gram (g) to Kilogram (kg)
        IF r_item.base_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01' THEN
            IF NOT EXISTS (SELECT 1 FROM inventory.item_uom_conversions WHERE item_id = r_item.id AND from_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02' AND to_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01') THEN
                INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), r_item.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', 0.0010, NOW(), 'system', NOW(), 'system');
            END IF;
        -- If base UOM is Lít (l), add conversion from Mililit (ml) to Lít (l)
        ELSIF r_item.base_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03' THEN
            IF NOT EXISTS (SELECT 1 FROM inventory.item_uom_conversions WHERE item_id = r_item.id AND from_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c04' AND to_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03') THEN
                INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), r_item.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c04', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c03', 0.0010, NOW(), 'system', NOW(), 'system');
            END IF;
        -- If base UOM is Gram (g), add conversion from Kilogram (kg) to Gram (g)
        ELSIF r_item.base_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02' THEN
            IF NOT EXISTS (SELECT 1 FROM inventory.item_uom_conversions WHERE item_id = r_item.id AND from_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01' AND to_uom_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02') THEN
                INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by, updated_at, updated_by)
                VALUES (gen_random_uuid(), r_item.id, '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c01', '000ebc99-9c0b-4ef8-bb6d-6bb9bd380c02', 1000.0000, NOW(), 'system', NOW(), 'system');
            END IF;
        END IF;

    END LOOP;
END $$;


-- 3.19 DYNAMIC SEED FOR COMPREHENSIVE MONTH-END STOCKTAKES (VERIFY ALL 35 PREMIUM INGREDIENTS)
DO $$
DECLARE
    v_st_comp_id UUID := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e99';
    v_st_draft_id UUID := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380e98';
    r_item RECORD;
    v_sys_qty NUMERIC(15,4);
    v_count_qty NUMERIC(15,4);
    v_variance NUMERIC(15,4);
    v_reason TEXT;
BEGIN
    -- A. Insert Completed Stocktake
    INSERT INTO inventory.stocktakes (id, name, status, snapshot_time, completed_at, notes, created_at, created_by, updated_at, updated_by)
    VALUES (v_st_comp_id, 'Kiểm kê định kỳ Kho Tổng & Bếp - Tháng 05/2026', 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1 DAY', CURRENT_TIMESTAMP - INTERVAL '1 DAY', 'Kiểm kê định kỳ đối soát thực tế toàn bộ 35 nguyên liệu chính.', NOW(), 'system', NOW(), 'system');

    -- B. Insert Draft Stocktake (Ongoing)
    INSERT INTO inventory.stocktakes (id, name, status, snapshot_time, completed_at, notes, created_at, created_by, updated_at, updated_by)
    VALUES (v_st_draft_id, 'Kiểm kê đột xuất Kho Quầy Bar - DRAFT', 'DRAFT', CURRENT_TIMESTAMP, NULL, 'Kiểm kê nhanh đột xuất các mặt hàng đồ uống đóng lon và đá viên.', NOW(), 'system', NOW(), 'system');

    -- C. Loop through all 35 ingredients to generate Completed Stocktake Items
    FOR r_item IN SELECT id, name, sku, category_id, base_uom_id FROM inventory.inventory_items LOOP
        -- 1. Calculate system quantity (sum of inventory levels)
        SELECT COALESCE(SUM(current_stock), 0.0) INTO v_sys_qty 
        FROM inventory.inventory_levels 
        WHERE item_id = r_item.id;

        -- If system quantity is 0 or unseeded, fallback to a realistic baseline
        IF v_sys_qty = 0 THEN
            v_sys_qty := 50.0;
        END IF;

        -- 2. Generate a highly realistic counted quantity and variance based on ingredient categories
        IF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a01' THEN
            -- Fresh Meat/Seafood/Veg: Small shrinkage/waste variance due to moisture loss/drip loss (approx. -0.5% to -1.5%)
            v_count_qty := ROUND(v_sys_qty * 0.992, 2);
            v_variance := v_count_qty - v_sys_qty;
            v_reason := 'Hao hụt tự nhiên do rã đông và bay hơi nước.';
        ELSIF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a02' THEN
            -- Starches/Dry: Tiny variance due to scooping/spillages
            IF r_item.sku IN ('RICE-ST25', 'SUGAR-WHITE-01', 'PANKO-POWDER') THEN
                v_count_qty := v_sys_qty - 0.25;
                v_reason := 'Hao hụt do đong đếm và rơi vãi nhỏ khi nấu.';
            ELSE
                v_count_qty := v_sys_qty;
                v_reason := 'Số liệu thực tế khớp 100% với hệ thống.';
            END IF;
            v_variance := v_count_qty - v_sys_qty;
        ELSE
            -- Packaged Drinks & Consumables: Usually 100% matching, occasionally a missing carton/unit
            IF r_item.sku = 'COCA-330' THEN
                v_count_qty := v_sys_qty - 1.0; -- Missing 1 lon
                v_reason := 'Mất mát 1 đơn vị chưa ghi nhận xuất kho.';
            ELSE
                v_count_qty := v_sys_qty;
                v_reason := 'Số liệu thực tế khớp 100% với hệ thống.';
            END IF;
            v_variance := v_count_qty - v_sys_qty;
        END IF;

        -- 3. Insert into stocktake_items for Completed Stocktake
        INSERT INTO inventory.stocktake_items (id, stocktake_id, item_id, system_quantity, counted_quantity, variance, adjustment_reason, created_at, created_by, updated_at, updated_by)
        VALUES (gen_random_uuid(), v_st_comp_id, r_item.id, v_sys_qty, v_count_qty, v_variance, v_reason, NOW(), 'system', NOW(), 'system');

        -- 4. For Drinks category, also populate the DRAFT stocktake items to show work-in-progress state
        IF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd380a03' THEN
            INSERT INTO inventory.stocktake_items (id, stocktake_id, item_id, system_quantity, counted_quantity, variance, adjustment_reason, created_at, created_by, updated_at, updated_by)
            VALUES (gen_random_uuid(), v_st_draft_id, r_item.id, v_sys_qty, NULL, NULL, NULL, NOW(), 'system', NOW(), 'system');
        END IF;
    END LOOP;

END $$;


