import uuid
import random
from datetime import datetime, timedelta

def get_uuid():
    return str(uuid.uuid4())

menu_items = [
    ("0f51b824-0dca-4cf5-a617-81f612355985", "Nước Ép Táo"),
    ("775d35d1-f3c8-4d58-b832-a03ec4b1cf51", "Nước Ép Dưa Hấu"),
    ("c3118fe7-3c06-4416-a28b-05e7b9731262", "Mojito Chanh Dây"),
    ("7304f8fc-4516-4a5d-90a1-9b8af31fe676", "Đá Xay Cà Phê"),
    ("4640555a-78dc-41b3-bc6a-6b2b4ab1bab1", "Matcha Đá Xay"),
    ("534fd5fe-9cd4-4100-942a-8033093e4b44", "Trà Lài Macchiato"),
    ("c5740a97-c517-4575-a890-71d530a736ae", "Sinh Tố Xoài"),
    ("285e86e5-5010-4115-bbfe-d6d7df3646a9", "Trà Dâu Tây"),
    ("3f0e1f39-e3a5-4cd4-af33-316988eac9f1", "Cold Brew"),
    ("0c6e0670-f4cb-4519-b83d-d130406ee88f", "Cacao Nóng"),
    ("8f870745-7141-4788-a0de-4291ed814300", "Sinh Tố Bơ"),
    ("9a1658a2-90a8-431e-bf47-3ad91eaaf9c1", "Khoai Tây Chiên"),
    ("59be1713-b9c8-495f-8689-cc3bab94f225", "Gà Rán Phần"),
    ("8c330c7b-b442-4454-b2d4-f92ff02fffc9", "Bánh Mì Quế"),
    ("77d3ca3d-99d4-4dd4-9147-d1ae5acf98bc", "Soup Cà Chua")
]

uoms = [
    (get_uuid(), "Kilogram", "kg"),
    (get_uuid(), "Gram", "g"),
    (get_uuid(), "Liter", "l"),
    (get_uuid(), "Milliliter", "ml"),
    (get_uuid(), "Box", "box"),
    (get_uuid(), "Piece", "pcs"),
    (get_uuid(), "Bag", "bag"),
    (get_uuid(), "Bottle", "btl"),
    (get_uuid(), "Pack", "pack"),
    (get_uuid(), "Can", "can")
]

uom_kg = uoms[0][0]
uom_g = uoms[1][0]
uom_l = uoms[2][0]
uom_ml = uoms[3][0]
uom_box = uoms[4][0]
uom_pcs = uoms[5][0]
uom_bag = uoms[6][0]

categories = [
    (get_uuid(), "Thịt & Hải sản"),
    (get_uuid(), "Rau củ quả"),
    (get_uuid(), "Nguyên liệu pha chế"),
    (get_uuid(), "Bao bì & Dụng cụ"),
    (get_uuid(), "Gia vị"),
    (get_uuid(), "Sản phẩm đóng chai")
]

cat_meat = categories[0][0]
cat_veg = categories[1][0]
cat_drink = categories[2][0]
cat_pack = categories[3][0]
cat_spice = categories[4][0]

suppliers = [
    (get_uuid(), "SUP-VISSAN", "Công ty Vissan", "0901234567"),
    (get_uuid(), "SUP-VINAMILK", "Vinamilk", "0912345678"),
    (get_uuid(), "SUP-TRUNGUYEN", "Cà phê Trung Nguyên", "0923456789"),
    (get_uuid(), "SUP-METRO", "Metro Cash & Carry", "0934567890"),
    (get_uuid(), "SUP-GREEN", "Trang trại Rau Sạch", "0945678901")
]

locations = [
    (get_uuid(), "Kho Tổng (Tầng Hầm)"),
    (get_uuid(), "Kho Bếp Lạnh"),
    (get_uuid(), "Kho Bếp Nóng"),
    (get_uuid(), "Quầy Pha Chế 1"),
    (get_uuid(), "Quầy Pha Chế 2")
]

inventory_items_data = [
    ("RAW-APPLE", "Táo tươi nhập khẩu", cat_veg, "RAW", uom_kg, 10.0, 45000),
    ("RAW-WATERMELON", "Dưa hấu", cat_veg, "RAW", uom_kg, 20.0, 15000),
    ("RAW-PASSION", "Chanh dây", cat_veg, "RAW", uom_kg, 5.0, 30000),
    ("RAW-COFFEE", "Cà phê hạt xay", cat_drink, "RAW", uom_kg, 15.0, 180000),
    ("RAW-MATCHA", "Bột trà xanh Matcha", cat_drink, "RAW", uom_kg, 5.0, 350000),
    ("RAW-JASMINE", "Trà Lài sấy khô", cat_drink, "RAW", uom_kg, 8.0, 220000),
    ("RAW-MANGO", "Xoài Cát Hòa Lộc", cat_veg, "RAW", uom_kg, 12.0, 60000),
    ("RAW-STRAWBERRY", "Dâu tây Đà Lạt", cat_veg, "RAW", uom_kg, 6.0, 120000),
    ("RAW-CACAO", "Bột Cacao nguyên chất", cat_drink, "RAW", uom_kg, 10.0, 250000),
    ("RAW-AVOCADO", "Bơ sáp", cat_veg, "RAW", uom_kg, 15.0, 80000),
    ("RAW-POTATO", "Khoai tây cắt lát đông lạnh", cat_veg, "RAW", uom_bag, 20.0, 90000),
    ("RAW-CHICKEN", "Thịt gà góc tư", cat_meat, "RAW", uom_kg, 30.0, 75000),
    ("RAW-BREAD", "Bánh mì sandwich", cat_pack, "RAW", uom_pcs, 50.0, 5000),
    ("RAW-TOMATO", "Cà chua chua ngọt", cat_veg, "RAW", uom_kg, 10.0, 25000),
    ("RAW-MILK", "Sữa tươi không đường", cat_drink, "RAW", uom_l, 40.0, 35000),
    ("RAW-SUGAR", "Đường kính trắng", cat_spice, "RAW", uom_kg, 25.0, 20000),
    ("RAW-SYRUP", "Syrup Đường đen", cat_drink, "RAW", uom_l, 10.0, 150000),
    ("RAW-ICE", "Đá viên sạch", cat_drink, "RAW", uom_kg, 50.0, 5000),
    ("PACK-CUP-PLASTIC", "Ly nhựa PET 500ml", cat_pack, "CONSUMABLE", uom_box, 10.0, 80000),
    ("PACK-CUP-PAPER", "Ly giấy Kraft 400ml", cat_pack, "CONSUMABLE", uom_box, 5.0, 120000),
    ("PACK-STRAW", "Ống hút giấy", cat_pack, "CONSUMABLE", uom_box, 10.0, 50000),
    ("PACK-BAG", "Túi chữ T", cat_pack, "CONSUMABLE", uom_box, 5.0, 40000),
    ("RAW-OIL", "Dầu ăn thực vật", cat_spice, "RAW", uom_l, 20.0, 45000),
    ("RAW-FLOUR", "Bột mì đa dụng", cat_spice, "RAW", uom_kg, 15.0, 22000),
    ("RAW-BUTTER", "Bơ lạt", cat_spice, "RAW", uom_kg, 8.0, 150000)
]

inventory_items = []
for row in inventory_items_data:
    item_id = get_uuid()
    inventory_items.append({
        "id": item_id,
        "sku": row[0],
        "name": row[1],
        "category_id": row[2],
        "type": row[3],
        "base_uom_id": row[4],
        "safety_stock": row[5],
        "avg_cost_price": row[6]
    })

sql = "-- ==========================================\n"
sql += "-- MASSIVE INVENTORY SEED DATA\n"
sql += "-- ==========================================\n\n"

sql += "TRUNCATE TABLE inventory.stock_transactions CASCADE;\n"
sql += "TRUNCATE TABLE inventory.stocktake_items CASCADE;\n"
sql += "TRUNCATE TABLE inventory.stocktakes CASCADE;\n"
sql += "TRUNCATE TABLE inventory.purchase_order_items CASCADE;\n"
sql += "TRUNCATE TABLE inventory.purchase_orders CASCADE;\n"
sql += "TRUNCATE TABLE inventory.recipe_items CASCADE;\n"
sql += "TRUNCATE TABLE inventory.recipes CASCADE;\n"
sql += "TRUNCATE TABLE inventory.item_uom_conversions CASCADE;\n"
sql += "TRUNCATE TABLE inventory.inventory_levels CASCADE;\n"
sql += "TRUNCATE TABLE inventory.inventory_items CASCADE;\n"
sql += "TRUNCATE TABLE inventory.locations CASCADE;\n"
sql += "TRUNCATE TABLE inventory.suppliers CASCADE;\n"
sql += "TRUNCATE TABLE inventory.item_categories CASCADE;\n"
sql += "TRUNCATE TABLE inventory.uoms CASCADE;\n\n"

# UOMs
sql += "INSERT INTO inventory.uoms (id, name, short_name, created_at, created_by) VALUES\n"
sql += ",\n".join([f"('{u[0]}', '{u[1]}', '{u[2]}', NOW(), 'system')" for u in uoms]) + ";\n\n"

# Categories
sql += "INSERT INTO inventory.item_categories (id, name, created_at, created_by) VALUES\n"
sql += ",\n".join([f"('{c[0]}', '{c[1]}', NOW(), 'system')" for c in categories]) + ";\n\n"

# Suppliers
sql += "INSERT INTO inventory.suppliers (id, code, name, phone, is_active, created_at, created_by) VALUES\n"
sql += ",\n".join([f"('{s[0]}', '{s[1]}', '{s[2]}', '{s[3]}', true, NOW(), 'system')" for s in suppliers]) + ";\n\n"

# Locations
sql += "INSERT INTO inventory.locations (id, name, is_active, created_at, created_by) VALUES\n"
sql += ",\n".join([f"('{l[0]}', '{l[1]}', true, NOW(), 'system')" for l in locations]) + ";\n\n"

# Inventory Items
sql += "INSERT INTO inventory.inventory_items (id, sku, name, category_id, type, base_uom_id, safety_stock, avg_cost_price, is_active, created_at, created_by) VALUES\n"
item_inserts = []
for i in inventory_items:
    item_inserts.append(f"('{i['id']}', '{i['sku']}', '{i['name']}', '{i['category_id']}', '{i['type']}', '{i['base_uom_id']}', {i['safety_stock']}, {i['avg_cost_price']}, true, NOW(), 'system')")
sql += ",\n".join(item_inserts) + ";\n\n"

# UOM Conversions
sql += "INSERT INTO inventory.item_uom_conversions (id, item_id, from_uom_id, to_uom_id, conversion_rate, created_at, created_by) VALUES\n"
conv_inserts = []
for i in inventory_items:
    # If base is kg, allow conversion from kg to g
    if i['base_uom_id'] == uom_kg:
        conv_inserts.append(f"('{get_uuid()}', '{i['id']}', '{uom_kg}', '{uom_g}', 1000, NOW(), 'system')")
    # If base is l, allow conversion from l to ml
    elif i['base_uom_id'] == uom_l:
        conv_inserts.append(f"('{get_uuid()}', '{i['id']}', '{uom_l}', '{uom_ml}', 1000, NOW(), 'system')")
sql += ",\n".join(conv_inserts) + ";\n\n"

# Inventory Levels
sql += "INSERT INTO inventory.inventory_levels (id, item_id, location_id, current_stock, allocated_stock, created_at, created_by) VALUES\n"
level_inserts = []
for i in inventory_items:
    loc1 = random.choice(locations)
    qty1 = random.uniform(20.0, 150.0)
    level_inserts.append(f"('{get_uuid()}', '{i['id']}', '{loc1[0]}', {qty1:.2f}, 0, NOW(), 'system')")
sql += ",\n".join(level_inserts) + ";\n\n"

# Recipes & Recipe Items
# Map 15 menu_items to recipes
sql += "INSERT INTO inventory.recipes (id, sale_item_id, type, created_at, created_by) VALUES\n"
recipe_inserts = []
recipes = []
for mi in menu_items:
    r_id = get_uuid()
    recipes.append((r_id, mi[0], mi[1]))
    recipe_inserts.append(f"('{r_id}', '{mi[0]}', 'STANDARD', NOW(), 'system')")
sql += ",\n".join(recipe_inserts) + ";\n\n"

sql += "INSERT INTO inventory.recipe_items (id, recipe_id, inventory_item_id, quantity, uom_id, wastage_percent, created_at, created_by) VALUES\n"
recipe_item_inserts = []
for r in recipes:
    # Randomly pick 3-4 ingredients
    num_ing = random.randint(2, 4)
    ings = random.sample(inventory_items, num_ing)
    for ing in ings:
        # Determine uom_id based on base
        if ing['base_uom_id'] == uom_kg:
            used_uom = uom_g
            qty = random.randint(10, 150)
        elif ing['base_uom_id'] == uom_l:
            used_uom = uom_ml
            qty = random.randint(20, 200)
        else:
            used_uom = ing['base_uom_id']
            qty = random.randint(1, 3)
        recipe_item_inserts.append(f"('{get_uuid()}', '{r[0]}', '{ing['id']}', {qty}, '{used_uom}', 0, NOW(), 'system')")
sql += ",\n".join(recipe_item_inserts) + ";\n\n"

# Purchase Orders
sql += "INSERT INTO inventory.purchase_orders (id, po_number, supplier_id, status, type, total_amount, created_at, created_by) VALUES\n"
po_inserts = []
pos = []
for i in range(1, 21):
    po_id = get_uuid()
    sup = random.choice(suppliers)
    status = random.choice(['DRAFT', 'PARTIAL', 'COMPLETED', 'CANCELLED'])
    amt = random.randint(1000000, 20000000)
    dt = datetime.now() - timedelta(days=random.randint(1, 60))
    pos.append((po_id, status))
    po_inserts.append(f"('{po_id}', 'PO-2026-{str(i).zfill(4)}', '{sup[0]}', '{status}', 'STANDARD', {amt}, '{dt.strftime('%Y-%m-%d %H:%M:%S')}', 'system')")
sql += ",\n".join(po_inserts) + ";\n\n"

sql += "INSERT INTO inventory.purchase_order_items (id, po_id, item_id, quantity, uom_id, unit_price, created_at, created_by) VALUES\n"
po_item_inserts = []
for po in pos:
    num_items = random.randint(2, 5)
    items = random.sample(inventory_items, num_items)
    for item in items:
        qty = random.randint(10, 100)
        po_item_inserts.append(f"('{get_uuid()}', '{po[0]}', '{item['id']}', {qty}, '{item['base_uom_id']}', {item['avg_cost_price']}, NOW(), 'system')")
sql += ",\n".join(po_item_inserts) + ";\n\n"

# Stocktakes
sql += "INSERT INTO inventory.stocktakes (id, completed_at, snapshot_time, status, created_at, created_by) VALUES\n"
st_inserts = []
sts = []
for i in range(10):
    st_id = get_uuid()
    status = random.choice(['DRAFT', 'COUNTING', 'COMPLETED', 'CANCELLED'])
    sts.append((st_id, status))
    dt = datetime.now() - timedelta(days=random.randint(1, 30))
    dt_str = f"'{dt.strftime('%Y-%m-%d %H:%M:%S')}'" if status == 'COMPLETED' else "NULL"
    st_inserts.append(f"('{st_id}', {dt_str}, '{dt.strftime('%Y-%m-%d %H:%M:%S')}', '{status}', '{dt.strftime('%Y-%m-%d %H:%M:%S')}', 'system')")
sql += ",\n".join(st_inserts) + ";\n\n"

sql += "INSERT INTO inventory.stocktake_items (id, stocktake_id, item_id, system_quantity, counted_quantity, variance, adjustment_reason, created_at, created_by) VALUES\n"
st_item_inserts = []
for st in sts:
    num_items = random.randint(5, 10)
    items = random.sample(inventory_items, num_items)
    for item in items:
        sys_qty = random.uniform(20.0, 100.0)
        counted = sys_qty + random.uniform(-5.0, 2.0)
        variance = counted - sys_qty
        st_item_inserts.append(f"('{get_uuid()}', '{st[0]}', '{item['id']}', {sys_qty:.2f}, {counted:.2f}, {variance:.2f}, 'Kiểm kê định kỳ', NOW(), 'system')")
sql += ",\n".join(st_item_inserts) + ";\n\n"

# Stock Transactions
sql += "INSERT INTO inventory.stock_transactions (id, item_id, transaction_type, quantity_change, reference_id, reason, unit_price_at_transaction, created_at, created_by) VALUES\n"
txn_inserts = []
for i in range(30):
    item = random.choice(inventory_items)
    ttype = random.choice(['IN_PO', 'OUT_SALE', 'OUT_WASTE', 'IN_TRANSFER', 'OUT_TRANSFER', 'ADJUSTMENT'])
    qty = random.uniform(1.0, 50.0)
    if 'OUT' in ttype:
        qty = -qty
    dt = datetime.now() - timedelta(days=random.randint(1, 60))
    txn_inserts.append(f"('{get_uuid()}', '{item['id']}', '{ttype}', {qty:.2f}, '{get_uuid()}', 'Giao dịch mẫu', {item['avg_cost_price']}, '{dt.strftime('%Y-%m-%d %H:%M:%S')}', 'system')")
sql += ",\n".join(txn_inserts) + ";\n\n"

with open("d:/srcDOAN/backend/inventory-service/src/main/resources/massive_seed_inventory.sql", "w", encoding="utf-8") as f:
    f.write(sql)
print("Massive seed SQL generated successfully!")
