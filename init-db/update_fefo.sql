ALTER TABLE inventory.inventory_levels DROP CONSTRAINT IF EXISTS uk48oac47m5odiq3veryon2t5bm;

CREATE TABLE IF NOT EXISTS inventory.inventory_batches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    item_id UUID NOT NULL REFERENCES inventory.inventory_items(id),
    lot_number VARCHAR(100),
    manufacture_date DATE,
    expiry_date DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(50),
    updated_by VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_inventory_batches_expiry ON inventory.inventory_batches(item_id, expiry_date);

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='inventory' AND table_name='inventory_levels' AND column_name='batch_id') THEN
        ALTER TABLE inventory.inventory_levels ADD COLUMN batch_id UUID REFERENCES inventory.inventory_batches(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='inventory' AND table_name='stock_transactions' AND column_name='location_id') THEN
        ALTER TABLE inventory.stock_transactions ADD COLUMN location_id UUID REFERENCES inventory.locations(id);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='inventory' AND table_name='stock_transactions' AND column_name='batch_id') THEN
        ALTER TABLE inventory.stock_transactions ADD COLUMN batch_id UUID REFERENCES inventory.inventory_batches(id);
    END IF;
END $$;

-- Seed some batches for testing
-- First, find an item id
DO $$ 
DECLARE
    v_item_id UUID;
    v_batch1_id UUID := gen_random_uuid();
    v_batch2_id UUID := gen_random_uuid();
BEGIN
    SELECT id INTO v_item_id FROM inventory.inventory_items LIMIT 1;
    
    IF v_item_id IS NOT NULL THEN
        -- Insert batches
        INSERT INTO inventory.inventory_batches (id, item_id, lot_number, expiry_date, is_active)
        VALUES 
            (v_batch1_id, v_item_id, 'LOT-001', CURRENT_DATE + INTERVAL '10 days', true),
            (v_batch2_id, v_item_id, 'LOT-002', CURRENT_DATE + INTERVAL '20 days', true)
        ON CONFLICT DO NOTHING;
        
        -- Insert levels for these batches
        INSERT INTO inventory.inventory_levels (id, item_id, batch_id, current_stock, allocated_stock)
        VALUES 
            (gen_random_uuid(), v_item_id, v_batch1_id, 10, 0),
            (gen_random_uuid(), v_item_id, v_batch2_id, 50, 0)
        ON CONFLICT DO NOTHING;
    END IF;
END $$;
