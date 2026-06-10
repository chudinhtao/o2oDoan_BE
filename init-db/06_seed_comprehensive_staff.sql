-- SQL Script to Seed Comprehensive Staff Data and Schedules for a 20-Table Restaurant
-- File: d:\srcDOAN\backend\init-db\06_seed_comprehensive_staff.sql
-- Synchronized with PostgreSQL Schema constraints and Java Entities
-- Completely cleans old shift templates, users, schedules, and attendance logs, then seeds exactly the 4 planned shift templates and 18 users.

-- 1. CLEAN UP ALL PREVIOUS DATA (COMPLETELY WIPE FOR A FRESH TEST GRID)
TRUNCATE TABLE auth.attendance_logs CASCADE;
TRUNCATE TABLE auth.work_schedules CASCADE;
TRUNCATE TABLE auth.shift_templates CASCADE;

-- Delete all users that do not belong to the 18 seeded accounts
DELETE FROM auth.users 
WHERE username NOT IN (
    'admin', 'admin2', 
    'cashier', 'cashier1', 'cashier2', 'cashier3', 
    'kitchen1', 'kitchen2', 'kitchen3', 'kitchen4', 
    'server1', 'server2', 'server3', 'server4', 'server5', 'server6', 'server7', 'server8'
);

-- 2. SEED CLEAN SHIFT TEMPLATES (EXACTLY 4 SHIFTS IN THE SYSTEM PLAN)
INSERT INTO auth.shift_templates (id, name, start_time, end_time, color_code, active, grace_period_minutes, created_at, updated_at) VALUES
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380001', 'Ca Sáng', '07:00:00', '12:00:00', '#60A5FA', TRUE, 5, NOW(), NOW()),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380003', 'Ca Chiều', '12:00:00', '17:00:00', '#FBBF24', TRUE, 5, NOW(), NOW()),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380004', 'Ca Tối', '17:00:00', '22:00:00', '#818CF8', TRUE, 5, NOW(), NOW()),
('000ebc99-9c0b-4ef8-bb6d-6bb9bd380005', 'Full-time', '08:00:00', '17:00:00', '#34D399', TRUE, 5, NOW(), NOW());

-- 3. SEED COMPREHENSIVE STAFF ACCOUNTS
-- Using ON CONFLICT (username) to allow safe execution multiple times
INSERT INTO auth.users (id, username, password, role, full_name, is_active, phone, created_at, updated_at) VALUES
('aaaa0000-0000-0000-0000-000000000001', 'admin', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'ADMIN', 'Quản Trị Viên', true, '0901234567', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000007', 'admin2', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'ADMIN', 'Nguyễn Quản Lý', true, '0901234568', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000002', 'cashier1', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'CASHIER', 'Thu Ngân Ca Sáng', true, '0912345671', NOW(), NOW()),
('bd5e1934-eaef-4908-bcff-69a7549646cd', 'cashier2', '$2a$10$b4RhJ6QX825K43JRI1K7Ueveh8.nii0o1pbdZ.v8mAN6.RXo9VDpy', 'CASHIER', 'Thu Ngân Ca Chiều', true, '0912345672', NOW(), NOW()),
('e1d7a8a7-a823-4dec-a8d0-5a9ae70a0d5d', 'cashier3', '$2a$10$l8H6h9mH2KNtzKNyOxD/UOLSLDDYBpGN7.n66oNIdUStDIqyPo/bW', 'CASHIER', 'Thu Ngân Ca Tối', true, '0912345673', NOW(), NOW()),
('c561dd2a-2e67-4030-a52e-b3f865736b0c', 'cashier', '$2a$10$.7eP/XPuGH.fWx1R5nSSfuGliXwWlLts8N4utfZJLKxg3yxpyvooe', 'CASHIER', 'Chu Đình Tạo', true, '0912345670', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000003', 'kitchen1', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'KITCHEN', 'Bếp Trưởng - Hot', true, '0922345671', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000008', 'kitchen2', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'KITCHEN', 'Bếp Phó - Cold', true, '0922345672', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000009', 'kitchen3', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'KITCHEN', 'Pha Chế - Beverage', true, '0922345673', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000010', 'kitchen4', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'KITCHEN', 'Phụ Bếp - Assistant', true, '0922345674', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000004', 'server1', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Sân Vườn', true, '0932345671', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000005', 'server2', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Tầng 1', true, '0932345672', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000006', 'server3', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Tầng 2', true, '0932345673', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000011', 'server4', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Ban Công', true, '0932345674', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000012', 'server5', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Ca Sáng', true, '0932345675', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000013', 'server6', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Ca Chiều', true, '0932345676', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000014', 'server7', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Ca Tối 01', true, '0932345677', NOW(), NOW()),
('aaaa0000-0000-0000-0000-000000000015', 'server8', '$2a$10$nTxRdt/tlHA.I1kILkQt5eUMdkbQQ25h1ET8.n2AzOuIEug/8MNc6', 'SERVER', 'Phục Vụ Ca Tối 02', true, '0932345678', NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET
  full_name = EXCLUDED.full_name,
  phone = EXCLUDED.phone,
  role = EXCLUDED.role,
  is_active = EXCLUDED.is_active,
  updated_at = NOW();

-- 4. SEED WORK SCHEDULES FOR THE OPERATIONAL STAFF
-- We will generate work schedules for all staff members from yesterday (CURRENT_DATE - 1) to next 5 days
DO $$
DECLARE
    r_user RECORD;
    v_date DATE;
    v_shift_id UUID;
    v_schedule_id UUID;
    v_status VARCHAR(20);
    v_check_in TIMESTAMP;
    v_check_out TIMESTAMP;
    v_is_late BOOLEAN;
    v_is_early BOOLEAN;
BEGIN
    -- Loop through all users
    FOR r_user IN (
        SELECT id, username, role FROM auth.users 
        WHERE username IN ('admin', 'admin2', 'cashier1', 'cashier2', 'cashier3', 'cashier', 'kitchen1', 'kitchen2', 'kitchen3', 'kitchen4', 'server1', 'server2', 'server3', 'server4', 'server5', 'server6', 'server7', 'server8')
    ) LOOP
        
        -- Determine shift_id based on username/role
        IF r_user.username IN ('cashier1', 'kitchen2', 'server1', 'server5') THEN
            v_shift_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380001'; -- Ca Sáng
        ELSIF r_user.username IN ('cashier2', 'kitchen3', 'server2', 'server6') THEN
            v_shift_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380003'; -- Ca Chiều
        ELSIF r_user.username IN ('cashier3', 'kitchen4', 'server3', 'server7', 'server8') THEN
            v_shift_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380004'; -- Ca Tối
        ELSE
            v_shift_id := '000ebc99-9c0b-4ef8-bb6d-6bb9bd380005'; -- Full-time
        END IF;

        -- Loop through a 7-day window: CURRENT_DATE - 1 to CURRENT_DATE + 5
        FOR i IN -1..5 LOOP
            v_date := CURRENT_DATE + i;
            
            -- Skip scheduling on Sundays for some variety (e.g. server5, server6, kitchen4 off on Sundays)
            IF EXTRACT(ISODOW FROM v_date) = 7 AND r_user.username IN ('server5', 'server6', 'kitchen4') THEN
                CONTINUE;
            END IF;

            -- Determine status
            IF v_date < CURRENT_DATE THEN
                v_status := 'COMPLETED';
            ELSIF v_date = CURRENT_DATE THEN
                v_status := 'PLANNED'; -- Will become COMPLETED if they checked in
            ELSE
                v_status := 'PLANNED';
            END IF;

            -- Create work schedule (Fresh insertion after cleanup)
            v_schedule_id := gen_random_uuid();
            INSERT INTO auth.work_schedules (id, user_id, shift_id, work_date, status, notes)
            VALUES (v_schedule_id, r_user.id, v_shift_id, v_date, v_status, 'Lịch phân công tự động');

            -- Seed Attendance Logs for Yesterday (COMPLETED)
            IF v_date = CURRENT_DATE - 1 THEN
                -- Random check-in check-out details
                IF r_user.username IN ('cashier1', 'kitchen2', 'server1', 'server5') THEN
                    -- Morning: Shift starts at 07:00, ends at 12:00
                    v_check_in := v_date + TIME '06:55:00' + (random() * INTERVAL '15 minutes'); -- 6:55 to 7:10
                    v_check_out := v_date + TIME '12:00:00' + (random() * INTERVAL '10 minutes');
                ELSIF r_user.username IN ('cashier2', 'kitchen3', 'server2', 'server6') THEN
                    -- Afternoon: Shift starts at 12:00, ends at 17:00
                    v_check_in := v_date + TIME '11:50:00' + (random() * INTERVAL '15 minutes'); -- 11:50 to 12:05
                    v_check_out := v_date + TIME '17:00:00' + (random() * INTERVAL '10 minutes');
                ELSIF r_user.username IN ('cashier3', 'kitchen4', 'server3', 'server7', 'server8') THEN
                    -- Evening: Shift starts at 17:00, ends at 22:00
                    v_check_in := v_date + TIME '16:50:00' + (random() * INTERVAL '15 minutes'); -- 16:50 to 17:05
                    v_check_out := v_date + TIME '22:00:00' + (random() * INTERVAL '10 minutes');
                ELSE
                    -- Full-time: Shift starts at 08:00, ends at 17:00
                    v_check_in := v_date + TIME '07:50:00' + (random() * INTERVAL '15 minutes'); -- 7:50 to 8:05
                    v_check_out := v_date + TIME '17:00:00' + (random() * INTERVAL '10 minutes');
                END IF;

                -- Check if late or early leave
                v_is_late := FALSE;
                IF EXTRACT(HOUR FROM v_check_in) > 7 AND r_user.username IN ('cashier1', 'kitchen2', 'server1', 'server5') THEN v_is_late := TRUE; END IF;
                IF EXTRACT(HOUR FROM v_check_in) > 12 AND r_user.username IN ('cashier2', 'kitchen3', 'server2', 'server6') THEN v_is_late := TRUE; END IF;
                IF EXTRACT(HOUR FROM v_check_in) > 17 AND r_user.username IN ('cashier3', 'kitchen4', 'server3', 'server7', 'server8') THEN v_is_late := TRUE; END IF;
                IF EXTRACT(HOUR FROM v_check_in) > 8 AND r_user.username IN ('admin', 'admin2', 'kitchen1', 'server4', 'cashier') THEN v_is_late := TRUE; END IF;

                -- Direct raw local timestamp insertion (pg will treat it relative to system PGTZ: Asia/Ho_Chi_Minh)
                INSERT INTO auth.attendance_logs (id, schedule_id, user_id, check_in, check_out, is_late, is_early_leave, check_in_note, check_out_note)
                VALUES (
                    gen_random_uuid(), 
                    v_schedule_id, 
                    r_user.id, 
                    v_check_in, 
                    v_check_out, 
                    v_is_late, 
                    FALSE, 
                    CASE WHEN v_is_late THEN 'Vào muộn do tắc đường' ELSE 'Đúng giờ' END,
                    'Hoàn thành ca làm việc'
                );
            END IF;

            -- Seed Attendance Logs for Today (Active/In progress check-ins)
            IF v_date = CURRENT_DATE THEN
                -- Morning shift has completed, afternoon shift is in progress or completed, evening is planned
                IF r_user.username IN ('cashier1', 'kitchen2', 'server1', 'server5') THEN
                    -- Morning: Checked in and out already
                    v_check_in := v_date + TIME '06:58:00';
                    v_check_out := v_date + TIME '12:01:00';
                    
                    INSERT INTO auth.attendance_logs (id, schedule_id, user_id, check_in, check_out, is_late, is_early_leave, check_in_note, check_out_note)
                    VALUES (gen_random_uuid(), v_schedule_id, r_user.id, v_check_in, v_check_out, FALSE, FALSE, 'Đúng ca', 'Về đúng giờ');
                    
                    UPDATE auth.work_schedules SET status = 'COMPLETED' WHERE id = v_schedule_id;
                
                ELSIF r_user.username IN ('cashier2', 'kitchen3', 'server2', 'server6') THEN
                    -- Afternoon: Checked in, not checked out yet
                    v_check_in := v_date + TIME '11:55:00';
                    
                    INSERT INTO auth.attendance_logs (id, schedule_id, user_id, check_in, check_out, is_late, is_early_leave, check_in_note)
                    VALUES (gen_random_uuid(), v_schedule_id, r_user.id, v_check_in, NULL, FALSE, FALSE, 'Check-in đúng giờ');
                END IF;
            END IF;

        END LOOP;
    END LOOP;
END $$;
