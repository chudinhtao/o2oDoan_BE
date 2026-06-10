import sys
import re

f1 = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\PurchaseOrderService.java'
with open(f1, 'r', encoding='utf-8') as file:
    content = file.read()

method = """
    @Transactional
    public PurchaseOrderResponse cancelPo(UUID id) {
        PurchaseOrder po = poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PO not found"));
        if (po.getStatus() != com.fnb.inventory.enums.POStatus.DRAFT) {
            throw new IllegalArgumentException("Chỉ có thể hủy phiếu nháp");
        }
        po.setStatus(com.fnb.inventory.enums.POStatus.CANCELLED);
        po = poRepository.save(po);
        return toResponse(po);
    }
"""
if 'public PurchaseOrderResponse cancelPo' not in content:
    content = content.replace('public PurchaseOrderResponse completePo(UUID id) {', method + '\n    public PurchaseOrderResponse completePo(UUID id) {')
    with open(f1, 'w', encoding='utf-8') as file:
        file.write(content)

f2 = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller\PurchaseOrderController.java'
with open(f2, 'r', encoding='utf-8') as file:
    content2 = file.read()

method2 = """
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelPo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy phiếu thành công", poService.cancelPo(id)));
    }
"""
if 'public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelPo' not in content2:
    content2 = content2.replace('public ResponseEntity<ApiResponse<PurchaseOrderResponse>> completePo', method2 + '\n    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> completePo')
    with open(f2, 'w', encoding='utf-8') as file:
        file.write(content2)

# Stocktake logic
f3 = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\StocktakeService.java'
with open(f3, 'r', encoding='utf-8') as file:
    content3 = file.read()

method3 = """
    @Transactional
    public StocktakeResponse cancelStocktake(UUID id) {
        Stocktake stocktake = stocktakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stocktake not found"));
        if (stocktake.getStatus() == com.fnb.inventory.enums.StocktakeStatus.COMPLETED) {
            throw new IllegalArgumentException("Không thể hủy đợt kiểm kê đã chốt");
        }
        stocktake.setStatus(com.fnb.inventory.enums.StocktakeStatus.CANCELLED);
        return toResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional
    public StocktakeResponse completeStocktake(UUID id) {
        Stocktake stocktake = stocktakeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Stocktake not found"));
        if (stocktake.getStatus() == com.fnb.inventory.enums.StocktakeStatus.COMPLETED) {
            throw new IllegalArgumentException("Đợt kiểm kê đã được chốt");
        }
        
        // Cập nhật tồn kho thực tế và tạo transaction
        for (StocktakeItem item : stocktake.getItems()) {
            if (item.getVariance() != null && item.getVariance().compareTo(java.math.BigDecimal.ZERO) != 0) {
                // Điều chỉnh tồn kho (cộng hoặc trừ)
                com.fnb.inventory.enums.TransactionType type = com.fnb.inventory.enums.TransactionType.ADJUSTMENT;
                
                // Fetch current item
                InventoryItem invItem = item.getItem();
                java.math.BigDecimal currentStock = invItem.getCurrentStock();
                java.math.BigDecimal newStock = item.getCountedQuantity() != null ? item.getCountedQuantity() : currentStock;
                
                // Update total stock
                invItem.setCurrentStock(newStock);
                
                // TODO: Update specific batch stock if necessary
                
                // Log transaction
                StockTransaction tx = StockTransaction.builder()
                        .item(invItem)
                        .type(type)
                        .referenceId(stocktake.getId().toString())
                        .quantityChanged(item.getVariance())
                        .stockAfter(newStock)
                        .note("Điều chỉnh sau kiểm kê " + stocktake.getId())
                        .build();
                transactionRepository.save(tx);
            }
        }
        
        stocktake.setStatus(com.fnb.inventory.enums.StocktakeStatus.COMPLETED);
        stocktake.setCompletedAt(java.time.LocalDateTime.now());
        return toResponse(stocktakeRepository.save(stocktake));
    }
"""

if 'public StocktakeResponse cancelStocktake' not in content3:
    content3 = content3.replace('public PageResponse<StocktakeResponse> getStocktakes', method3 + '\n    public PageResponse<StocktakeResponse> getStocktakes')
    with open(f3, 'w', encoding='utf-8') as file:
        file.write(content3)

f4 = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\controller\StocktakeController.java'
with open(f4, 'r', encoding='utf-8') as file:
    content4 = file.read()

method4 = """
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> cancelStocktake(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy kiểm kê thành công", stocktakeService.cancelStocktake(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> completeStocktake(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Chốt sổ kiểm kê thành công", stocktakeService.completeStocktake(id)));
    }
"""

if 'public ResponseEntity<ApiResponse<StocktakeResponse>> cancelStocktake' not in content4:
    content4 = content4.replace('public ResponseEntity<ApiResponse<StocktakeResponse>> updateCounts', method4 + '\n    public ResponseEntity<ApiResponse<StocktakeResponse>> updateCounts')
    with open(f4, 'w', encoding='utf-8') as file:
        file.write(content4)
