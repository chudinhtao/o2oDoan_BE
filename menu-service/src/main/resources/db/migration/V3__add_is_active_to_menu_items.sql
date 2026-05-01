-- V3: Add is_active column to menu_items (for soft delete support)
ALTER TABLE menu.menu_items ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT true;
