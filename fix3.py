import os

# Fix PurchaseOrderService
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\PurchaseOrderService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('? "IN_QUICK" : TransactionType.IN_PO', '? TransactionType.IN_QUICK : TransactionType.IN_PO')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix StockTransactionController
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller\StockTransactionController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('request.setTransactionType("OUT_WASTE");', 'request.setTransactionType(TransactionType.OUT_WASTE);')
content = content.replace('.transactionType("ADJUSTMENT")', '.transactionType(TransactionType.ADJUSTMENT)')
content = content.replace('.transactionType("IN_QUICK")', '.transactionType(TransactionType.IN_QUICK)')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix PurchaseOrderController
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller\PurchaseOrderController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String status, @RequestParam(required = false) String type', 'POStatus status, @RequestParam(required = false) POType type')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix AdminInventoryController
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller\AdminInventoryController.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String type,', 'ItemType type,')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix InventoryItemService
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\InventoryItemService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String type, Boolean isActive', 'ItemType type, Boolean isActive')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix InventoryItemRepository
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\repository\InventoryItemRepository.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String type,', 'ItemType type,')
if 'import com.fnb.inventory.enums.ItemType;' not in content:
    content = content.replace('import org.springframework.data.domain.Pageable;', 'import org.springframework.data.domain.Pageable;\nimport com.fnb.inventory.enums.ItemType;')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix PurchaseOrderRepository
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\repository\PurchaseOrderRepository.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String status, @Param("type") String type', 'POStatus status, @Param("type") POType type')
if 'import com.fnb.inventory.enums.*' not in content:
    content = content.replace('import org.springframework.data.jpa.repository.JpaRepository;', 'import org.springframework.data.jpa.repository.JpaRepository;\nimport com.fnb.inventory.enums.*;')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

# Fix PurchaseOrderService getPos
filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\PurchaseOrderService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('String status, String type', 'POStatus status, POType type')
with open(filepath, 'w', encoding='utf-8') as f:
    f.write(content)

