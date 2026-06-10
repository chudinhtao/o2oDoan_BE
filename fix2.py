import os, glob

root_dir = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory'

for filepath in glob.glob(root_dir + '/**/*.java', recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    orig = content
    
    # RecipeService/Repository
    content = content.replace('List<RecipeResponse> findAllByType(String type)', 'List<RecipeResponse> findAllByType(RecipeType type)')
    content = content.replace('List<Recipe> findByType(String type)', 'List<Recipe> findByType(RecipeType type)')
    
    # PurchaseOrderService
    # line 114: bad type in conditional expression
    content = content.replace('TransactionType.IN_PO : "QUICK_GRN"', 'TransactionType.IN_PO : TransactionType.IN_QUICK')
    content = content.replace('TransactionType.IN_PO : POType.QUICK_GRN.name()', 'TransactionType.IN_PO : TransactionType.IN_QUICK')

    # StockTransactionController
    content = content.replace('RequestParam(required = false) String type', 'RequestParam(required = false) TransactionType type')
    content = content.replace('StockTransactionRequest request, String type', 'StockTransactionRequest request, TransactionType type')
    content = content.replace('TransactionType.valueOf(type)', 'type')

    # OrderEventConsumerService
    content = content.replace('String transactionType', 'TransactionType transactionType')
    content = content.replace('recordStockTransaction(item, quantity, TransactionType.HOLD, referenceId, orderLineItemId, reason)', 'recordStockTransaction(item, quantity, TransactionType.HOLD, referenceId, orderLineItemId, reason)')
    content = content.replace('void recordStockTransaction(InventoryItem item, java.math.BigDecimal quantity, String type', 'void recordStockTransaction(InventoryItem item, java.math.BigDecimal quantity, TransactionType type')
    content = content.replace('void recordStockTransaction(InventoryItem item, BigDecimal quantityChange, String transactionType', 'void recordStockTransaction(InventoryItem item, BigDecimal quantityChange, TransactionType transactionType')

    # General TransactionType passing
    content = content.replace(', String transactionType,', ', TransactionType transactionType,')

    # Fix bad replace in StockTransactionService if any
    
    if content != orig:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print("Updated:", filepath)
