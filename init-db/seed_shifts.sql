TRUNCATE TABLE auth.shift_templates CASCADE;

INSERT INTO auth.shift_templates (id, name, start_time, end_time, color_code, active, grace_period_minutes) VALUES
(gen_random_uuid(), 'Ca Sáng (Full)', '06:00:00', '14:00:00', '#10B981', true, 15),
(gen_random_uuid(), 'Ca Chiều (Full)', '14:00:00', '22:00:00', '#F59E0B', true, 15),
(gen_random_uuid(), 'Ca Đêm (Full)', '22:00:00', '06:00:00', '#8B5CF6', true, 15),
(gen_random_uuid(), 'Ca Hành Chính', '08:00:00', '17:00:00', '#3B82F6', true, 15),
(gen_random_uuid(), 'Ca Gãy Sáng (PT)', '06:00:00', '10:00:00', '#34D399', true, 10),
(gen_random_uuid(), 'Ca Gãy Trưa (PT)', '10:00:00', '14:00:00', '#FBBF24', true, 10),
(gen_random_uuid(), 'Ca Gãy Chiều (PT)', '14:00:00', '18:00:00', '#EC4899', true, 10),
(gen_random_uuid(), 'Ca Gãy Tối (PT)', '18:00:00', '22:00:00', '#EF4444', true, 10);
