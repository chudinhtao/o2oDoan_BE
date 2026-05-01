-- Migration do ai-service yêu cầu, chạy thủ công hoặc thêm vào flyway của menu-service
-- Thêm cột embedding (vector 384 chiều cho model all-MiniLM-L6-v2) vào bảng menu_items

-- Bước 1: Bật extension pgvector nếu chưa có
CREATE EXTENSION IF NOT EXISTS vector;

-- Bước 2: Thêm cột embedding vào menu_items
ALTER TABLE menu.menu_items
    ADD COLUMN IF NOT EXISTS embedding vector(384);

-- Bước 3: Tạo index HNSW để tăng tốc tìm kiếm vector
-- HNSW nhanh hơn IVFFlat cho dataset nhỏ-vừa (< 1 triệu records)
CREATE INDEX IF NOT EXISTS idx_menu_items_embedding_hnsw
    ON menu.menu_items
    USING hnsw (embedding vector_cosine_ops);

-- Ghi chú: Sau khi chạy script này, gọi API nội bộ để đồng bộ vector:
-- POST http://localhost:8087/api/internal/ai/sync-menu-vectors
