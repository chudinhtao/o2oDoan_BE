-- Dọn dẹp data cũ (nếu muốn, nhưng ở đây chỉ cần INSERT thêm)
-- DELETE FROM menu.promotions;

-- 1. Giảm 10% toàn bộ hoá đơn (Tự động)
WITH p1 AS (
    INSERT INTO menu.promotions (id, code, name, scope, trigger_type, discount_type, discount_value, max_discount, usage_limit, used_count, priority, is_stackable, is_active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'AUTO10', 'Giảm 10% Tổng Bill', 'ORDER', 'AUTO', 'PERCENT', 10.00, 50000.00, 1000, 0, 1, true, true, NOW(), NOW())
    RETURNING id
)
INSERT INTO menu.promotion_targets (id, promotion_id, target_type)
SELECT gen_random_uuid(), id, 'GLOBAL' FROM p1;

-- 2. Mã Code GIAM50K giảm thẳng 50k cho đơn từ 200k
WITH p2 AS (
    INSERT INTO menu.promotions (id, code, name, scope, trigger_type, discount_type, discount_value, usage_limit, used_count, priority, is_stackable, is_active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'GIAM50K', 'Giảm 50K cho đơn từ 200K', 'ORDER', 'COUPON', 'FIX_AMOUNT', 50000.00, 500, 0, 2, false, true, NOW(), NOW())
    RETURNING id
)
, trg2 AS (
    INSERT INTO menu.promotion_targets (id, promotion_id, target_type)
    SELECT gen_random_uuid(), id, 'GLOBAL' FROM p2
    RETURNING promotion_id
)
INSERT INTO menu.promotion_requirements (id, promotion_id, min_order_amount, min_quantity)
SELECT gen_random_uuid(), promotion_id, 200000.00, 0 FROM trg2;

-- 3. Giảm 20% cho Category Lẩu & Nướng (000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003)
WITH p3 AS (
    INSERT INTO menu.promotions (id, code, name, scope, trigger_type, discount_type, discount_value, max_discount, usage_limit, used_count, priority, is_stackable, is_active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'LAUNUONG20', 'Giảm 20% Lẩu Nướng', 'PRODUCT', 'AUTO', 'PERCENT', 20.00, 100000.00, 500, 0, 3, true, true, NOW(), NOW())
    RETURNING id
)
INSERT INTO menu.promotion_targets (id, promotion_id, target_type, target_id)
SELECT gen_random_uuid(), id, 'CATEGORY', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38a003'::uuid FROM p3;

-- 4. Tặng Coca Cola Lon miễn phí (Giảm 100% cho item 000ebc99-9c0b-4ef8-bb6d-6bb9bd38f406) - Coupon FREECOCA
WITH p4 AS (
    INSERT INTO menu.promotions (id, code, name, scope, trigger_type, discount_type, discount_value, usage_limit, used_count, priority, is_stackable, is_active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'FREECOCA', 'Tặng Coca Cola', 'PRODUCT', 'COUPON', 'PERCENT', 100.00, 200, 0, 4, true, true, NOW(), NOW())
    RETURNING id
)
INSERT INTO menu.promotion_targets (id, promotion_id, target_type, target_id)
SELECT gen_random_uuid(), id, 'ITEM', '000ebc99-9c0b-4ef8-bb6d-6bb9bd38f406'::uuid FROM p4;

-- 5. Giảm 15% VIP Members (Yêu cầu GOLD member)
WITH p5 AS (
    INSERT INTO menu.promotions (id, code, name, scope, trigger_type, discount_type, discount_value, max_discount, usage_limit, used_count, priority, is_stackable, is_active, created_at, updated_at)
    VALUES (gen_random_uuid(), 'VIP15', 'Giảm 15% cho VIP GOLD', 'ORDER', 'AUTO', 'PERCENT', 15.00, 200000.00, 1000, 0, 5, true, true, NOW(), NOW())
    RETURNING id
)
, trg5 AS (
    INSERT INTO menu.promotion_targets (id, promotion_id, target_type)
    SELECT gen_random_uuid(), id, 'GLOBAL' FROM p5
    RETURNING promotion_id
)
INSERT INTO menu.promotion_requirements (id, promotion_id, min_order_amount, min_quantity, member_level)
SELECT gen_random_uuid(), promotion_id, 0.00, 0, 'GOLD' FROM trg5;
