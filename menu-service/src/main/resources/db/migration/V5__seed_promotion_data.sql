SET search_path TO menu;

-- Xóa dữ liệu cũ nếu có (để đảm bảo script chạy sạch)
DELETE FROM promotions WHERE code IN ('CHAOHE', 'FNB50', 'VIPONLY', 'FREESHIP');

INSERT INTO promotions (id, code, name, type, value, min_order_amount, max_discount_value, usage_limit, used_count, is_active, start_at, end_at)
VALUES
    -- Giảm 20%, tối đa 30k, cho đơn từ 100k
    (gen_random_uuid(), 'CHAOHE', 'Mừng hè rực rỡ', 'PERCENT', 20.00, 100000.00, 30000.00, 100, 0, TRUE, now(), now() + interval '3 months'),
    
    -- Giảm thẳng 50k cho đơn từ 200k
    (gen_random_uuid(), 'FNB50', 'Ưu đãi bạn mới', 'AMOUNT', 50000.00, 200000.00, NULL, 50, 0, TRUE, now(), now() + interval '1 month'),
    
    -- Siêu giảm giá 50%, tối đa 100k, cho đơn lớn từ 500k
    (gen_random_uuid(), 'VIPONLY', 'Đại tiệc VIP', 'PERCENT', 50.00, 500000.00, 100000.00, 10, 0, TRUE, now(), now() + interval '7 days'),
    
    -- Giảm 10%, không giới hạn số tiền giảm (thử nghiệm)
    (gen_random_uuid(), 'FREESHIP', 'Mã dùng thử', 'PERCENT', 10.00, 0.00, 0.00, 1000, 0, TRUE, now(), now() + interval '1 year');
