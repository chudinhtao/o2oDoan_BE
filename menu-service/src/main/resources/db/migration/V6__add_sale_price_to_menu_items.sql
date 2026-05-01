ALTER TABLE menu.menu_items ADD COLUMN sale_price DECIMAL(12, 2);
COMMENT ON COLUMN menu.menu_items.sale_price IS 'Giá khuyến mãi dành cho Flash Sale/Giảm giá trực tiếp';
