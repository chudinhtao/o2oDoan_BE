ALTER TABLE menu.promotions ADD COLUMN IF NOT EXISTS max_discount_value DECIMAL(12, 2);
ALTER TABLE menu.promotions ADD COLUMN IF NOT EXISTS usage_limit INTEGER;
ALTER TABLE menu.promotions ADD COLUMN IF NOT EXISTS used_count INTEGER DEFAULT 0;
