import sys
import re

f1 = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\PurchaseOrderService.java'
with open(f1, 'r', encoding='utf-8') as file:
    content = file.read()

# Fix PurchaseOrderService
content = re.sub(
    r'@Transactional\s*@Transactional\s*public PurchaseOrderResponse cancelPo',
    r'@Transactional\n    public PurchaseOrderResponse cancelPo',
    content
)

# It was:
# @Transactional
#     @Transactional
#     public PurchaseOrderResponse cancelPo
# Wait, let's just do a blanket fix by matching the exact broken strings

def fix_po_service():
    with open(f1, 'r', encoding='utf-8') as file:
        lines = file.readlines()
    
    out = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if '@Transactional' in line and i+1 < len(lines) and '@Transactional' in lines[i+1]:
            # skip the first one
            i += 1
            continue
        
        # fix: public PurchaseOrderResponse completePo is missing its @Transactional?
        # my python script did: replace('public PurchaseOrderResponse completePo(UUID id) {', method + '\n    public PurchaseOrderResponse completePo(UUID id) {')
        # So completePo still has its @Transactional above the injected cancelPo!
        out.append(line)
        i += 1
    
    # Actually, it's easier to just use regex to clean up
fix_po_service()
