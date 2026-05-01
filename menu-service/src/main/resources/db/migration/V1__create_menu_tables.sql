-- ============================================================
-- V1: Schema menu — categories, menu_items, options, promotions
-- ============================================================
CREATE SCHEMA IF NOT EXISTS menu;
SET search_path TO menu;

CREATE TABLE categories (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100) NOT NULL,
    image_url     VARCHAR(500),
    display_order INT DEFAULT 0,
    is_active     BOOLEAN DEFAULT TRUE
);

CREATE TABLE menu_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id   UUID REFERENCES categories(id) ON DELETE SET NULL,
    name          VARCHAR(200) NOT NULL,
    description   TEXT,
    image_url     VARCHAR(500),
    base_price    DECIMAL(12,2) NOT NULL CHECK (base_price >= 0),
    station       VARCHAR(20) NOT NULL CHECK (station IN ('HOT','COLD','DRINK')),
    is_available  BOOLEAN DEFAULT TRUE,
    is_featured   BOOLEAN DEFAULT FALSE,
    created_at    TIMESTAMP DEFAULT NOW(),
    updated_at    TIMESTAMP DEFAULT NOW()
);

CREATE TABLE item_option_groups (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id       UUID NOT NULL REFERENCES menu_items(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    type          VARCHAR(10) NOT NULL CHECK (type IN ('SINGLE','MULTI')),
    is_required   BOOLEAN DEFAULT FALSE,
    display_order INT DEFAULT 0
);

CREATE TABLE item_options (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id      UUID NOT NULL REFERENCES item_option_groups(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    extra_price   DECIMAL(12,2) DEFAULT 0,
    is_available  BOOLEAN DEFAULT TRUE
);

CREATE TABLE promotions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code              VARCHAR(50) UNIQUE,
    name              VARCHAR(200) NOT NULL,
    type              VARCHAR(20) NOT NULL CHECK (type IN ('PERCENT','AMOUNT','TIME','AUTO')),
    value             DECIMAL(12,2),
    min_order_amount  DECIMAL(12,2) DEFAULT 0,
    min_quantity      INT DEFAULT 0,
    start_at          TIMESTAMP,
    end_at            TIMESTAMP,
    is_active         BOOLEAN DEFAULT TRUE,
    conditions        JSONB,
    created_at        TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_categories_active       ON categories(is_active);
CREATE INDEX idx_menu_items_category     ON menu_items(category_id);
CREATE INDEX idx_menu_items_available    ON menu_items(is_available, is_featured);
CREATE INDEX idx_option_groups_item      ON item_option_groups(item_id);
CREATE INDEX idx_item_options_group      ON item_options(group_id);
CREATE INDEX idx_promotions_code         ON promotions(code) WHERE code IS NOT NULL;
CREATE INDEX idx_promotions_active       ON promotions(is_active, start_at, end_at);
