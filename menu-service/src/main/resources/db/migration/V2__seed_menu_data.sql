-- ============================================================
-- V2: Seed data menu — categories và menu_items mẫu
-- ============================================================
SET search_path TO menu;

INSERT INTO categories (id, name, display_order) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Món chính',    1),
    ('00000000-0000-0000-0000-000000000002', 'Khai vị',      2),
    ('00000000-0000-0000-0000-000000000003', 'Đồ uống',      3),
    ('00000000-0000-0000-0000-000000000004', 'Tráng miệng',  4);

INSERT INTO menu_items (category_id, name, description, base_price, station, is_featured) VALUES
    ('00000000-0000-0000-0000-000000000001', 'Cơm sườn nướng',      'Cơm trắng + sườn nướng mật ong',    55000, 'HOT',   TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Bún bò Huế',           'Bún bò cay đặc trưng xứ Huế',       45000, 'HOT',   TRUE),
    ('00000000-0000-0000-0000-000000000001', 'Phở bò tái',           'Phở bò nước trong, tái nạm',        50000, 'HOT',   FALSE),
    ('00000000-0000-0000-0000-000000000002', 'Gỏi cuốn tôm thịt',    '2 cuốn + tương hoisin',             35000, 'COLD',  FALSE),
    ('00000000-0000-0000-0000-000000000002', 'Chả giò chiên',        '5 cuốn + tương chua ngọt',          40000, 'HOT',   FALSE),
    ('00000000-0000-0000-0000-000000000003', 'Cà phê sữa đá',       'Robusta đậm đà + sữa đặc',          25000, 'DRINK', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'Trà sữa trân châu',    'Trà Ô long + trân châu đen',        40000, 'DRINK', TRUE),
    ('00000000-0000-0000-0000-000000000003', 'Nước ép cam',          '100% cam tươi',                     35000, 'DRINK', FALSE),
    ('00000000-0000-0000-0000-000000000004', 'Chè bà ba',            'Chè thập cẩm truyền thống',         30000, 'COLD',  FALSE),
    ('00000000-0000-0000-0000-000000000004', 'Kem dừa',              'Kem dừa tươi + thạch',              35000, 'COLD',  FALSE);
