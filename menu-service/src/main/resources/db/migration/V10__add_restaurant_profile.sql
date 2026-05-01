-- V10: Add restaurant_profile table (Singleton pattern)
CREATE TABLE IF NOT EXISTS menu.restaurant_profile (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255)    NOT NULL DEFAULT 'Nhà Hàng ABC',
    slogan      VARCHAR(512),
    logo_url    TEXT,
    banner_url  TEXT,
    address     VARCHAR(512),
    phone       VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Seed default profile (Singleton row)
INSERT INTO menu.restaurant_profile (name, slogan, address, phone)
VALUES (
    'Ngọc Mai F&B',
    'Tinh hoa ẩm thực Việt',
    '123 Đường Ẩm Thực, TP. Hồ Chí Minh',
    '0901234567'
)
ON CONFLICT DO NOTHING;
