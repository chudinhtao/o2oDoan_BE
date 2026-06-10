DO $$
DECLARE
    r_level RECORD;
    v_batch_id UUID;
BEGIN
    FOR r_level IN SELECT * FROM inventory.inventory_levels WHERE batch_id IS NULL LOOP
        v_batch_id := gen_random_uuid();
        
        INSERT INTO inventory.inventory_batches (id, item_id, lot_number, manufacture_date, expiry_date, is_active)
        VALUES (
            v_batch_id, 
            r_level.item_id, 
            'LOT-AUTO-' || substring(v_batch_id::text from 1 for 6), 
            CURRENT_DATE, 
            CURRENT_DATE + INTERVAL '1 year', 
            TRUE
        );
        
        UPDATE inventory.inventory_levels 
        SET batch_id = v_batch_id 
        WHERE id = r_level.id;
    END LOOP;
END $$;
