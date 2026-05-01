-- =================================================================================
-- V9: REFACTOR PROMOTION SYSTEM
-- Xóa bảng promotions cũ (JSONB-based), tạo lại schema chuẩn hóa 5 bảng
-- Hỗ trợ 5 scopes: PRODUCT, ORDER, BUNDLE, COUPON, TIME-BASED
-- =================================================================================

-- 1. Xóa bảng cũ (CASCADE để xóa FK phụ thuộc nếu có)
DROP TABLE IF EXISTS menu.promotions CASCADE;

-- 2. Tạo lại bảng promotions chuẩn hóa
CREATE TABLE menu.promotions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code            VARCHAR(50) UNIQUE,
    name            VARCHAR(200) NOT NULL,

    scope           VARCHAR(20)  NOT NULL CHECK (scope IN ('PRODUCT', 'ORDER', 'BUNDLE')),
    trigger_type    VARCHAR(20)  NOT NULL CHECK (trigger_type IN ('AUTO', 'COUPON')),
    discount_type   VARCHAR(20)  NOT NULL CHECK (discount_type IN ('PERCENT', 'FIX_AMOUNT', 'FIX_PRICE')),

    discount_value  DECIMAL(12, 2),
    max_discount    DECIMAL(12, 2),

    usage_limit     INT,
    used_count      INT          NOT NULL DEFAULT 0,
    priority        INT          NOT NULL DEFAULT 0,

    start_at        TIMESTAMP,
    end_at          TIMESTAMP,

    is_stackable    BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- 3. Bảng targets: phạm vi áp dụng (ITEM / CATEGORY / GLOBAL)
CREATE TABLE menu.promotion_targets (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id    UUID NOT NULL REFERENCES menu.promotions(id) ON DELETE CASCADE,
    target_type     VARCHAR(20) NOT NULL CHECK (target_type IN ('ITEM', 'CATEGORY', 'GLOBAL')),
    target_id       UUID
);

-- 4. Bảng bundle_items: cấu trúc Combo / Mua X tặng Y
CREATE TABLE menu.promotion_bundle_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id    UUID NOT NULL REFERENCES menu.promotions(id) ON DELETE CASCADE,
    item_id         UUID NOT NULL REFERENCES menu.menu_items(id) ON DELETE CASCADE,
    quantity        INT  NOT NULL DEFAULT 1,
    role            VARCHAR(10) NOT NULL CHECK (role IN ('BUY', 'GET'))
);

-- 5. Bảng requirements: điều kiện (đơn tối thiểu, số lượng)
CREATE TABLE menu.promotion_requirements (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id      UUID NOT NULL REFERENCES menu.promotions(id) ON DELETE CASCADE,
    min_order_amount  DECIMAL(12, 2) NOT NULL DEFAULT 0,
    min_quantity      INT            NOT NULL DEFAULT 0,
    member_level      VARCHAR(50)
);

-- 6. Bảng schedules: khung giờ Happy Hour
CREATE TABLE menu.promotion_schedules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    promotion_id    UUID NOT NULL REFERENCES menu.promotions(id) ON DELETE CASCADE,
    day_of_week     INT  NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    start_time      TIME NOT NULL,
    end_time        TIME NOT NULL
);

-- 7. Indexes để Pricing Engine query nhanh
CREATE INDEX idx_promotions_active      ON menu.promotions (is_active, start_at, end_at);
CREATE INDEX idx_promotions_scope       ON menu.promotions (scope, is_active);
CREATE INDEX idx_promotions_code        ON menu.promotions (code) WHERE code IS NOT NULL;
CREATE INDEX idx_promo_targets_type     ON menu.promotion_targets (target_type, target_id);
CREATE INDEX idx_promo_bundle_item      ON menu.promotion_bundle_items (item_id);
