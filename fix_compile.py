import os, glob

service_dir = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service'
controller_dir = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller'

def replace_in_file(filepath):
    if not os.path.exists(filepath): return
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original = content
    
    # Add imports
    if 'com.fnb.inventory.enums.' not in content:
        imports = 'import com.fnb.inventory.enums.*;\n'
        if 'import org.springframework' in content:
            content = content.replace('import org.springframework', imports + 'import org.springframework', 1)
        else:
            content = content.replace('import java.util', imports + 'import java.util', 1)

    # DTO updates where .name() was used if getters still return String
    # But getters now return Enums because we updated DTOs.
    content = content.replace('request.getType()', 'request.getType()') # no change if it returns Enum
    content = content.replace('request.getStatus()', 'request.getStatus()')
    content = content.replace('request.getTransactionType()', 'request.getTransactionType()')

    # Replace literal strings
    content = content.replace('"MAIN_ITEM"', 'RecipeType.MAIN_ITEM')
    content = content.replace('"MODIFIER"', 'RecipeType.MODIFIER')
    
    # In PurchaseOrderService
    if 'PurchaseOrderService.java' in filepath:
        content = content.replace('"DRAFT"', 'POStatus.DRAFT')
        content = content.replace('"COMPLETED"', 'POStatus.COMPLETED')
        content = content.replace('"IN_PO"', 'TransactionType.IN_PO')
        content = content.replace('"STANDARD"', 'POType.STANDARD')
        content = content.replace('"QUICK_GRN"', 'POType.QUICK_GRN')

    # In StocktakeService
    if 'StocktakeService.java' in filepath:
        content = content.replace('"DRAFT"', 'StocktakeStatus.DRAFT')
        content = content.replace('"IN_PROGRESS"', 'StocktakeStatus.IN_PROGRESS')
        content = content.replace('"COMPLETED"', 'StocktakeStatus.COMPLETED')
        content = content.replace('"ADJUSTMENT"', 'TransactionType.ADJUSTMENT')

    # In StockTransactionService
    if 'StockTransactionService.java' in filepath:
        content = content.replace('"IN_PO"', 'TransactionType.IN_PO')
        content = content.replace('"IN_QUICK"', 'TransactionType.IN_QUICK')
        content = content.replace('"OUT_SALE"', 'TransactionType.OUT_SALE')
        content = content.replace('"OUT_WASTE"', 'TransactionType.OUT_WASTE')
        content = content.replace('"OUT_TRANSFER"', 'TransactionType.OUT_TRANSFER')
        content = content.replace('"HOLD"', 'TransactionType.HOLD')
        content = content.replace('"REFUND"', 'TransactionType.REFUND')
        content = content.replace('"ADJUSTMENT"', 'TransactionType.ADJUSTMENT')
        content = content.replace('"MANUAL_BLOCK"', 'TransactionType.MANUAL_BLOCK')
        
    # In OrderEventConsumerService
    if 'OrderEventConsumerService.java' in filepath:
        content = content.replace('"HOLD"', 'TransactionType.HOLD')
        content = content.replace('"OUT_SALE"', 'TransactionType.OUT_SALE')
        content = content.replace('"REFUND"', 'TransactionType.REFUND')

    # StockTransactionController
    if 'StockTransactionController.java' in filepath:
        content = content.replace('String type', 'TransactionType type')

    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f'Updated {filepath}')

for f in glob.glob(service_dir + '/*.java'):
    replace_in_file(f)
for f in glob.glob(controller_dir + '/*.java'):
    replace_in_file(f)
