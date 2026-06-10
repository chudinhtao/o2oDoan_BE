-- SQL Migration for Staff Management (HRM)
-- Schema: auth

-- 1. Bảng danh mục Ca làm việc (Shift Templates)
CREATE TABLE IF NOT EXISTS auth.shift_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    color_code VARCHAR(10) DEFAULT '#3B82F6', -- Màu hiển thị trên lịch
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Lịch phân ca (Work Schedules)
CREATE TABLE IF NOT EXISTS auth.work_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    shift_id UUID NOT NULL REFERENCES auth.shift_templates(id),
    work_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PLANNED', -- PLANNED, COMPLETED, CANCELLED
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_user_shift_date UNIQUE (user_id, work_date)
);

-- 3. Bảng Nhật ký chấm công (Attendance Logs)
CREATE TABLE IF NOT EXISTS auth.attendance_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    schedule_id UUID REFERENCES auth.work_schedules(id) ON DELETE SET NULL,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    check_in TIMESTAMP WITH TIME ZONE,
    check_out TIMESTAMP WITH TIME ZONE,
    check_in_note TEXT,
    check_out_note TEXT,
    is_late BOOLEAN DEFAULT FALSE,
    is_early_leave BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index để tối ưu truy vấn
CREATE INDEX IF NOT EXISTS idx_work_schedules_user_date ON auth.work_schedules(user_id, work_date);
CREATE INDEX IF NOT EXISTS idx_attendance_logs_user_date ON auth.attendance_logs(user_id, check_in);

-- Seed data cơ bản (Ca làm việc mẫu cho quán ăn)
INSERT INTO auth.shift_templates (name, start_time, end_time, color_code) VALUES
('Ca Sáng', '07:00:00', '12:00:00', '#60A5FA'),
('Ca Trưa (Peak)', '11:00:00', '14:00:00', '#F87171'),
('Ca Chiều', '12:00:00', '17:00:00', '#FBBF24'),
('Ca Tối', '17:00:00', '22:00:00', '#818CF8'),
('Full-time', '08:00:00', '17:00:00', '#34D399')
ON CONFLICT DO NOTHING;
