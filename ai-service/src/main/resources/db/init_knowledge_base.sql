-- Tao schema ai neu chua co
CREATE SCHEMA IF NOT EXISTS ai;

CREATE TABLE IF NOT EXISTS ai.knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(384)
);

-- Seed data for F&B standards
INSERT INTO ai.knowledge_base (category, title, content) VALUES
('BENCHMARK', 'Ty le huy don (Cancel Rate)', 'Trong nganh F&B, ty le huy don an toan phai duoi 3%. Neu vuot qua 5%, day la tin hieu canh bao do cho thay quy trinh bep, nguyen lieu hoac chat luong phuc vu dang co van de nghiem trong. Can kiem tra ngay cac don bi huy vi ly do "Het mon" hoac "Khach doi qua lau".'),
('BENCHMARK', 'Chi phi nguyen lieu (Food Cost)', 'Food cost tieu chuan cho nha hang nen dao dong tu 25% den 30% tong doanh thu cua mon an. Neu food cost vuot 35%, nha hang se bi thu hep bien loi nhuan. Giai phap la thuong luong lai voi NCC hoac giam khau phan/doi cong thuc.'),
('BENCHMARK', 'Ty le nghi viec (Staff Turnover)', 'Ty le nghi viec cua nhan su nganh F&B thuong kha cao, tuy nhien nen giu muc duoi 15% mot nam doi voi khoi van hanh (bep/quan ly) va duoi 30% voi khoi phuc vu. Neu cao hon, thuong do che do luong hoac moi truong lam viec qua ap luc.'),
('MENU_ENGINEERING', 'Phan tich BCG Matrix cho thuc don', 'Phan loai mon an thanh 4 nhom: 
- STAR (Ngoi sao): Doanh so cao, loi nhuan cao. -> Can duy tri chat luong, giu vung vi the.
- PLOWHORSE (Ngua cay): Doanh so cao, loi nhuan thap. -> Nen tang gia mot chut hoac giam chi phi nguyen lieu, giam khau phan.
- PUZZLE (Cau do): Doanh so thap, loi nhuan cao. -> Can tang cuong marketing, lam noi bat tren menu, push sale.
- DOG (Cho muc): Doanh so thap, loi nhuan thap. -> Nen loai bo khoi menu de tiet kiem chi phi van hanh.')
ON CONFLICT DO NOTHING;

-- [Phase 4.2] Tao bang Semantic Caching cho Admin AI
CREATE TABLE IF NOT EXISTS ai.admin_semantic_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    embedding vector(384),
    answer TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

