-- Idempotent script to ensure every menu item has at least one option group ('Size') and options ('Size M', 'Size L')
DO $$
DECLARE
    r_item RECORD;
    v_group_id UUID;
    v_added_count INT := 0;
BEGIN
    FOR r_item IN SELECT id, name FROM menu.menu_items LOOP
        -- Check if this item already has any option group
        IF NOT EXISTS (SELECT 1 FROM menu.item_option_groups WHERE item_id = r_item.id) THEN
            -- Generate a new UUID for the group
            v_group_id := gen_random_uuid();
            
            -- Insert the option group ('Size' of type 'SINGLE')
            INSERT INTO menu.item_option_groups (id, item_id, name, type, is_required, display_order)
            VALUES (v_group_id, r_item.id, 'Size', 'SINGLE', TRUE, 0);
            
            -- Insert standard options: Size M (0 extra cost) and Size L (10000 extra cost)
            INSERT INTO menu.item_options (id, group_id, name, extra_price, is_available)
            VALUES 
                (gen_random_uuid(), v_group_id, 'Size M', 0.00, TRUE),
                (gen_random_uuid(), v_group_id, 'Size L', 10000.00, TRUE);
                
            v_added_count := v_added_count + 1;
            RAISE NOTICE 'Added Size option group and options for item: % (%)', r_item.name, r_item.id;
        END IF;
    END LOOP;
    
    RAISE NOTICE 'Successfully supplemented % items with standard options.', v_added_count;
END $$;
