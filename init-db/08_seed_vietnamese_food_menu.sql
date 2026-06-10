-- SQL Script to seed Premium Vietnamese Restaurant Menu & Options
-- Completely replaces the old cafe menu with high-quality Vietnamese dishes

-- 1. CLEAN EXISTING MENU DATA
TRUNCATE TABLE menu.item_options CASCADE;
TRUNCATE TABLE menu.item_option_groups CASCADE;
TRUNCATE TABLE menu.promotion_bundle_items CASCADE;
TRUNCATE TABLE menu.promotion_targets CASCADE;
TRUNCATE TABLE menu.promotions CASCADE;
TRUNCATE TABLE menu.menu_items CASCADE;
TRUNCATE TABLE menu.categories CASCADE;

-- 2. SEED CLEAN CATEGORIES FOR VIETNAMESE DINER
INSERT INTO menu.categories (id, name, display_order, is_active, tax_rate, image_url) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Món Khai Vị & Ăn Nhẹ', 1, TRUE, 8.00, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Cơm & Mỳ - Phở', 2, TRUE, 8.00, 'https://images.unsplash.com/photo-1569562211093-4ed0d0758f12?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Món Đặc Biệt - Lẩu & Nướng', 3, TRUE, 10.00, 'https://images.unsplash.com/photo-1555939594-58d7cb561ad1?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Món Tráng Miệng', 4, TRUE, 10.00, 'https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Đồ Uống Giải Khát', 5, TRUE, 10.00, 'https://images.unsplash.com/photo-1497534446932-c925b458314e?w=500&auto=format&fit=crop&q=60');

-- 3. SEED 40 PREMIUM VIETNAMESE MENU ITEMS
-- Keeping IDs of inventory-linked items: Gà Rán Phần (59be1713-b9c8-495f-8689-cc3bab94f225) & Khoai Tây Chiên (9a1658a2-90a8-431e-bf47-3ad91eaaf9c1)

-- 3.1 Món Khai Vị & Ăn Nhẹ (8 items)
INSERT INTO menu.menu_items (id, category_id, name, description, base_price, station, is_available, is_featured, is_active, tax_rate, image_url) VALUES
('59be1713-b9c8-495f-8689-cc3bab94f225', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Gà Rán Phần', 'Gà chiên giòn rụm bên ngoài, mọng nước bên trong, ăn kèm tương ớt.', 25000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1569058242253-92a9c755a0ec?w=500&auto=format&fit=crop&q=60'),
('9a1658a2-90a8-431e-bf47-3ad91eaaf9c1', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Khoai Tây Chiên', 'Khoai tây cắt lát chiên vàng giòn, xóc muối bơ tỏi thơm lừng.', 35000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1573080496219-bb080dd4f877?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f001', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Nem Rán Hà Nội', 'Nem rán nhân thịt heo, miến, mộc nhĩ giòn rụm chấm nước mắm chua ngọt.', 45000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1606491956689-2ea866880c84?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f002', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Bánh Mì Muối Ớt', 'Bánh mì nướng bơ muối ớt giòn rụm, thêm chà bông, mỡ hành và xốt trứng muối.', 30000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1601050690597-df056fb4ce78?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f003', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Salad Ức Gà Áp Chảo', 'Rau xà lách sạch Đà Lạt trộn dầu giấm, kết hợp ức gà áp chảo và sốt mè rang.', 55000.00, 'COLD', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1546069901-ba9599a7e63c?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f004', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Ngô Chiên Bơ Tỏi', 'Ngô ngọt tách hạt chiên xóc bơ tỏi thơm nức mũi.', 30000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1514516345957-556ca7d90a29?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f005', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Gỏi Cuốn Tôm Thịt', 'Bánh tráng cuốn tôm tươi, thịt ba chỉ luộc, rau sống chấm tương đậu phộng.', 40000.00, 'COLD', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1534422298391-e4f8c172dddb?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f006', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a001', 'Súp Măng Tây Cua', 'Súp nóng hổi béo ngậy với thịt cua bể tươi và măng tây xắt nhỏ.', 45000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1547592165-e1d17f1a0655?w=500&auto=format&fit=crop&q=60');

-- 3.2 Cơm & Mỳ - Phở (8 items)
INSERT INTO menu.menu_items (id, category_id, name, description, base_price, station, is_available, is_featured, is_active, tax_rate, image_url) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f101', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Phở Bò Tái Lăn', 'Phở truyền thống với thịt bò u vai xào lăn tỏi thơm nức, nước dùng ngọt xương thanh thanh.', 65000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f102', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Phở Gà Ta Cổ Điển', 'Bánh phở mềm cùng thịt gà ta xé phay da giòn vàng, thêm lá chanh thơm mát.', 55000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1625398407796-82650a8c135f?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f103', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Cơm Rang Dưa Bò', 'Cơm đảo giòn hạt vàng óng, ăn kèm đĩa thịt bò xào dưa chua đậm vị gia đình.', 60000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f104', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Bún Chả Hà Nội', 'Thịt viên và thịt ba chỉ nướng than hoa thơm lừng trong bát nước mắm pha ấm nóng.', 60000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1596560548464-f010689b7087?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f105', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Cơm Tấm Sườn Bì Chả', 'Cơm tấm chuẩn vị Sài Gòn ăn kèm miếng sườn nướng mọng nước, bì thính, chả trứng và đồ chua.', 65000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1541696432-82c6da8ce7bf?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f106', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Bún Bò Huế Đặc Biệt', 'Bún sợi to dai mềm cùng thịt nạm bò, giò heo chân giò hầm nhừ, chả cua và nước dùng sả ruốc đậm đà.', 65000.00, 'HOT', TRUE, TRUE, TRUE, 8.00, 'https://images.unsplash.com/photo-1625398407796-82650a8c135f?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f107', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Mỳ Quảng Gà Trứng', 'Sợi mỳ vàng óng quyện nước dùng gà cô đặc ngọt béo, ăn kèm gà kho tiêu, trứng cút và bánh đa.', 55000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1625398407796-82650a8c135f?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f108', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', 'Cơm Gà Hải Nam Dẻo Thơm', 'Thịt gà luộc thảo mộc mềm ngậy xếp trên lớp cơm hạt dẻo nấu từ nước dùng gà và mỡ hành.', 65000.00, 'HOT', TRUE, FALSE, TRUE, 8.00, 'https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500&auto=format&fit=crop&q=60');

-- 3.3 Món Đặc Biệt - Lẩu & Nướng (8 items)
INSERT INTO menu.menu_items (id, category_id, name, description, base_price, station, is_available, is_featured, is_active, tax_rate, image_url) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f201', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Lẩu Thái Hải Sản (Nhỏ)', 'Nồi lẩu Thái chua cay nóng hổi, ăn kèm tôm, mực tươi, ngao, nghêu và rau nấm.', 250000.00, 'HOT', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1547592165-e1d17f1a0655?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f202', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Lẩu Riêu Cua Sườn Sụn (Nhỏ)', 'Nước lẩu riêu cua đồng xịn chưng gạch béo ngậy, ăn kèm thịt bò Mỹ, sườn sụn non và giò tai.', 280000.00, 'HOT', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1547592165-e1d17f1a0655?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f203', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Bò Nướng Tảng Sốt Tiêu', 'Miếng thăn bò lớn tẩm ướp tiêu đen nướng chín tái mềm mọng, xắt miếng trực tiếp tại bàn.', 190000.00, 'HOT', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f204', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Sườn Nướng BBQ Tảng M', 'Tảng sườn heo tuyển chọn sốt ướp BBQ bí truyền nướng chậm vàng đều óng ả.', 180000.00, 'HOT', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f205', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Gà Nướng Mắc Khén Tây Bắc', 'Gà đồi ướp hạt mắc khén thơm cay đặc trưng Tây Bắc nướng da giòn rúm.', 160000.00, 'HOT', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f206', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Cá Quả Nướng Riềng Sả M', 'Cá quả cuốn giấy bạc nướng thơm mùi sả riềng, cuốn bánh tráng rau sống.', 170000.00, 'HOT', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f207', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Lẩu Nấm Chay Thanh Đạm M', 'Nước dùng lẩu hầm rau củ ngọt thanh nhẹ nhàng kết hợp cùng 8 loại nấm quý tươi ngon.', 220000.00, 'HOT', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1547592165-e1d17f1a0655?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f208', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003', 'Mực Trứng Nướng Sa Tế M', 'Mực trứng tươi béo tẩm sa tế cay nồng nướng vàng thơm, chấm muối tiêu chanh.', 150000.00, 'HOT', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500&auto=format&fit=crop&q=60');

-- 3.4 Món Tráng Miệng (8 items)
INSERT INTO menu.menu_items (id, category_id, name, description, base_price, station, is_available, is_featured, is_active, tax_rate, image_url) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f301', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Chè Bưởi An Giang', 'Chè bưởi nấu từ cùi bưởi giòn sần sật bọc bột lọc, nước cốt dừa béo ngậy hạt đậu xanh dẻo thơm.', 25000.00, 'COLD', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1508737027454-e6454ef45afd?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f302', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Chè Thái Sầu Riêng M', 'Chè Thái trái cây mít nhãn thạch rau câu quyện sốt sầu riêng Ri6 thơm nức.', 35000.00, 'COLD', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1508737027454-e6454ef45afd?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f303', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Bánh Flan Trà Xanh M', 'Bánh caramen mềm mịn kết hợp hương trà xanh Uji dịu mát và nước đường đắng nhẹ.', 25000.00, 'COLD', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f304', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Sữa Yogurt Nếp Cẩm', 'Sữa chua dẻo nhà làm kết hợp nếp cẩm Điện Biên ngọt bùi lên men tự nhiên.', 30000.00, 'COLD', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1488477181946-6428a0291777?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f305', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Kem Dừa Côn Đảo', 'Quả dừa xiêm chứa 3 viên kem dừa thơm ngậy, rắc lạc rang và dừa khô xắt sợi.', 45000.00, 'COLD', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1497034825429-c343d7c6a68f?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f306', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Rau Câu Dừa Xiêm M', 'Thạch rau câu đổ nguyên chất từ nước dừa tươi thanh ngọt sảng khoái.', 25000.00, 'COLD', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f307', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Hoa Quả Tươi Theo Mùa M', 'Đĩa dưa hấu, xoài cát, mít nghệ, dứa tươi mọng chấm muối ớt Tây Ninh cực bắt vị.', 40000.00, 'COLD', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1519996521430-02b798c1d881?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f308', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a004', 'Bánh Su Kem Mini', 'Set 4 chiếc bánh su vỏ mềm dai chứa đầy nhân kem vani mát lạnh tan chảy.', 20000.00, 'COLD', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1551024601-bec78aea704b?w=500&auto=format&fit=crop&q=60');

-- 3.5 Đồ Uống Giải Khát (8 items)
INSERT INTO menu.menu_items (id, category_id, name, description, base_price, station, is_available, is_featured, is_active, tax_rate, image_url) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f401', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Trà Đá Mát Lạnh', 'Trà đá thơm mùi trà xanh Thái Nguyên thanh lọc sảng khoái.', 5000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f402', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Nước Mía Siêu Sạch', 'Nước mía nguyên chất ép cùng quả tắc thơm mát ngọt ngào nhiều đá.', 15000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f403', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Trà Tắc Mật Ong Vàng', 'Sự kết hợp hoàn hảo giữa trà nhài, nước cốt tắc tươi và mật ong hoa nhãn ngọt thơm.', 20000.00, 'DRINK', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1497534446932-c925b458314e?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f404', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Sinh Tố Xoài Cát Chu', 'Xoài chín Cát Chu ngọt đậm xay với sữa đặc và đá bào nhuyễn béo ngậy.', 35000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1553530666-ba11a7da3888?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f405', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Nước Dừa Xiêm Tươi', 'Dừa xiêm Bến Tre chặt cả quả ngọt lịm tự nhiên thơm thanh mát.', 30000.00, 'DRINK', TRUE, TRUE, TRUE, 10.00, 'https://images.unsplash.com/photo-1556881286-fc6915169721?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f406', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Coca Cola Lon', 'Lon Coca Cola 330ml ướp lạnh sẵn.', 15000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f407', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Bia Sài Gòn Chill Lạnh', 'Bia Sài Gòn Chill mát lạnh cực sảng khoái bên bạn bè gia đình.', 25000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1608270586620-248524c67de9?w=500&auto=format&fit=crop&q=60'),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd38f408', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a005', 'Nước Cam Vắt Tự Nhiên', 'Nước cam vắt nguyên chất từ 3 trái cam sành chín mọng mật ong dạt dào.', 30000.00, 'DRINK', TRUE, FALSE, TRUE, 10.00, 'https://images.unsplash.com/photo-1621506289937-a8e4df240d0b?w=500&auto=format&fit=crop&q=60');


-- 4. SEED DYNAMIC & REALISTIC OPTIONS FOR DISHES/DRINKS
-- Iterating through items and adding specific options

DO $$
DECLARE
    r_item RECORD;
    v_group_id UUID;
BEGIN
    FOR r_item IN SELECT id, name, category_id, station FROM menu.menu_items LOOP
        -- Case A: Beverages (DRINK station) -> Add Sugar & Ice option group
        IF r_item.station = 'DRINK' AND r_item.name != 'Trà Đá Mát Lạnh' THEN
            v_group_id := gen_random_uuid();
            INSERT INTO menu.item_option_groups (id, item_id, name, type, is_required, display_order)
            VALUES (v_group_id, r_item.id, 'Độ Ngọt & Đá', 'SINGLE', TRUE, 0);
            
            INSERT INTO menu.item_options (id, group_id, name, extra_price, is_available)
            VALUES 
                (gen_random_uuid(), v_group_id, 'Đá & Đường Bình Thường', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Ít Đá & Ít Đường', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Không Đá & Không Đường', 0.00, TRUE);
                
        -- Case B: Main course dishes (Rice, Noodles & Pho or Hotpot/Grill) -> Add Spiciness or Extra toppings
        ELSIF r_item.category_id IN ('000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003') THEN
            -- 1. Spiciness level (Single choice)
            v_group_id := gen_random_uuid();
            INSERT INTO menu.item_option_groups (id, item_id, name, type, is_required, display_order)
            VALUES (v_group_id, r_item.id, 'Mức Độ Cay', 'SINGLE', TRUE, 0);
            
            INSERT INTO menu.item_options (id, group_id, name, extra_price, is_available)
            VALUES 
                (gen_random_uuid(), v_group_id, 'Không Cay', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Cay Vừa', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Cay Nhiều', 0.00, TRUE);
                
            -- 2. Extra toppings for Rice & Noodles (Multi choice)
            IF r_item.category_id = '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a002' THEN
                v_group_id := gen_random_uuid();
                INSERT INTO menu.item_option_groups (id, item_id, name, type, is_required, display_order)
                VALUES (v_group_id, r_item.id, 'Thêm Topping', 'MULTI', FALSE, 1);
                
                INSERT INTO menu.item_options (id, group_id, name, extra_price, is_available)
                VALUES 
                    (gen_random_uuid(), v_group_id, 'Thêm Trứng Lòng Đào', 5000.00, TRUE),
                    (gen_random_uuid(), v_group_id, 'Thêm Thịt Bò Xắt Lát', 15000.00, TRUE),
                    (gen_random_uuid(), v_group_id, 'Thêm Chả Giò Chiên', 10000.00, TRUE);
            END IF;
            
        -- Case C: Appetizers & Desserts (Appetizers or Desserts) -> Add Size option group
        ELSE
            v_group_id := gen_random_uuid();
            INSERT INTO menu.item_option_groups (id, item_id, name, type, is_required, display_order)
            VALUES (v_group_id, r_item.id, 'Kích Thước', 'SINGLE', TRUE, 0);
            
            INSERT INTO menu.item_options (id, group_id, name, extra_price, is_available)
            VALUES 
                (gen_random_uuid(), v_group_id, 'Size Vừa', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Size Lớn', 10000.00, TRUE);
        END IF;
    END LOOP;
END $$;
