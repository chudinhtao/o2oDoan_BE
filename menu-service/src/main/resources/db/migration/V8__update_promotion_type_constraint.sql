-- Drop the existing check constraint on type and recreate it with FLASH_SALE
ALTER TABLE menu.promotions DROP CONSTRAINT IF EXISTS promotions_type_check;

ALTER TABLE menu.promotions ADD CONSTRAINT promotions_type_check CHECK (type IN ('PERCENT','AMOUNT','TIME','AUTO','FLASH_SALE'));
