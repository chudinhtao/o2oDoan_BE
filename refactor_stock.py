import os

filepath = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\StockTransactionService.java'
with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# We want to replace everything from "@Transactional\n    public StockTransactionResponse recordTransaction"
# down to the end of "applyKillSwitch" method.

import re

# Find start of recordTransaction
start_idx = content.find('@Transactional\n    public StockTransactionResponse recordTransaction')

# Find start of getTransactionHistory (the method AFTER applyKillSwitch)
end_idx = content.find('@Transactional(readOnly = true)\n    public PageResponse<StockTransactionResponse> getTransactionHistory')

if start_idx != -1 and end_idx != -1:
    new_methods = """@Transactional
    public StockTransactionResponse recordTransaction(StockTransactionRequest request) {
        log.info("Recording stock transaction for item {}: {} ({})", 
                request.getItemId(), request.getTransactionType(), request.getQuantityChange());

        InventoryItem item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Nguyên liệu không tồn tại"));

        java.util.List<InventoryLevel> levels = levelRepository.findByItemId(item.getId());
        if (levels.isEmpty()) {
            InventoryLevel newLevel = InventoryLevel.builder().item(item).build();
            levels.add(levelRepository.save(newLevel));
        }

        BigDecimal totalOldStock = levels.stream()
                .map(InventoryLevel::getCurrentStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 1. Ghi sổ cái
        StockTransaction transaction = StockTransaction.builder()
                .item(item)
                .transactionType(request.getTransactionType())
                .quantityChange(request.getQuantityChange())
                .unitPriceAtTransaction(request.getUnitPriceAtTransaction())
                .referenceId(request.getReferenceId())
                .orderLineItemId(request.getOrderLineItemId())
                .reason(request.getReason())
                .build();
        transaction = transactionRepository.save(transaction);

        BigDecimal remainingQty = request.getQuantityChange();

        // 2. Cập nhật tồn kho theo FEFO (nếu xuất) hoặc cộng dồn (nếu nhập)
        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            // Nhập kho (Cộng vào lô mặc định nếu request không có batchId)
            InventoryLevel targetLevel = levels.get(0);
            targetLevel.setCurrentStock(targetLevel.getCurrentStock().add(remainingQty));
            levelRepository.save(targetLevel);
            
            // Tính MAC
            if (TransactionType.IN_PO.equals(request.getTransactionType()) || TransactionType.IN_QUICK.equals(request.getTransactionType())) {
                if (request.getUnitPriceAtTransaction() != null && request.getUnitPriceAtTransaction().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal oldAvgCost = item.getAvgCostPrice() != null ? item.getAvgCostPrice() : BigDecimal.ZERO;
                    BigDecimal oldTotalValue = totalOldStock.max(BigDecimal.ZERO).multiply(oldAvgCost);
                    BigDecimal newTotalValue = request.getQuantityChange().multiply(request.getUnitPriceAtTransaction());
                    
                    BigDecimal newStockTotal = totalOldStock.max(BigDecimal.ZERO).add(request.getQuantityChange());
                    if (newStockTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal newAvgCost = oldTotalValue.add(newTotalValue).divide(newStockTotal, 2, java.math.RoundingMode.HALF_UP);
                        item.setAvgCostPrice(newAvgCost);
                        itemRepository.save(item);
                        log.info("Cập nhật giá vốn Moving Average cho {}: {}", item.getName(), newAvgCost);
                    }
                }
            }
        } else if (remainingQty.compareTo(BigDecimal.ZERO) < 0) {
            // Xuất kho (FEFO: ưu tiên còn hạn, ngày hết hạn gần nhất)
            java.util.List<InventoryLevel> availableLevels = levels.stream()
                    .filter(l -> l.getCurrentStock().compareTo(BigDecimal.ZERO) > 0)
                    .sorted((l1, l2) -> {
                        java.time.LocalDate d1 = (l1.getBatch() != null) ? l1.getBatch().getExpiryDate() : null;
                        java.time.LocalDate d2 = (l2.getBatch() != null) ? l2.getBatch().getExpiryDate() : null;
                        if (d1 == null && d2 == null) return 0;
                        if (d1 == null) return 1;
                        if (d2 == null) return -1;
                        return d1.compareTo(d2);
                    })
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal qtyToDeduct = remainingQty.abs();

            for (InventoryLevel lvl : availableLevels) {
                if (qtyToDeduct.compareTo(BigDecimal.ZERO) <= 0) break;

                // Log cảnh báo nếu lô này đã hết hạn
                if (lvl.getBatch() != null && lvl.getBatch().getExpiryDate() != null) {
                    if (lvl.getBatch().getExpiryDate().isBefore(java.time.LocalDate.now())) {
                        log.warn("CẢNH BÁO: Đang trừ kho vào lô đã HẾT HẠN (Batch: {}, Expiry: {}) cho nguyên liệu: {}", 
                            lvl.getBatch().getLotNumber(), lvl.getBatch().getExpiryDate(), item.getName());
                    }
                }

                BigDecimal deductible = lvl.getCurrentStock().min(qtyToDeduct);
                lvl.setCurrentStock(lvl.getCurrentStock().subtract(deductible));
                levelRepository.save(lvl);
                qtyToDeduct = qtyToDeduct.subtract(deductible);
            }

            // Nếu vẫn còn thiếu (qtyToDeduct > 0), trừ vào lô mặc định tạo tồn kho âm
            if (qtyToDeduct.compareTo(BigDecimal.ZERO) > 0) {
                InventoryLevel fallbackLevel = levels.stream().filter(l -> l.getBatch() == null).findFirst().orElse(levels.get(0));
                fallbackLevel.setCurrentStock(fallbackLevel.getCurrentStock().subtract(qtyToDeduct));
                levelRepository.save(fallbackLevel);
                log.warn("Nguyên liệu {} đã bị TRỪ ÂM KHO số lượng: {}", item.getName(), qtyToDeduct);
            }
        }

        BigDecimal totalNewStock = totalOldStock.add(request.getQuantityChange());

        // 3. O2O Kill-switch
        if (totalNewStock.compareTo(BigDecimal.ZERO) <= 0 && request.getQuantityChange().signum() < 0) {
            log.warn("Nguyên liệu {} đã hết hàng, kích hoạt Kill-switch cho các món liên quan!", item.getName());
            eventPublisher.publishEvent(new com.fnb.inventory.dto.event.InventoryOutOfStockEvent(item.getId(), item.getName()));
        }

        // 4. O2O In-Stock
        boolean wasOutOfStock = totalOldStock.compareTo(BigDecimal.ZERO) <= 0;
        boolean nowInStock = totalNewStock.compareTo(BigDecimal.ZERO) > 0;
        boolean isInboundTransaction = TransactionType.IN_PO.equals(request.getTransactionType())
                || TransactionType.IN_QUICK.equals(request.getTransactionType())
                || TransactionType.ADJUSTMENT.equals(request.getTransactionType());

        if (wasOutOfStock && nowInStock && isInboundTransaction) {
            log.info("Nguyên liệu {} có hàng trở lại ({} → {}), kích hoạt mở lại các món liên quan.",
                    item.getName(), totalOldStock, totalNewStock);
            eventPublisher.publishEvent(new com.fnb.inventory.dto.event.InventoryInStockEvent(item.getId(), item.getName()));
        }

        return mapToResponse(transaction);
    }

    /**
     * Kill-switch thủ công: Set tồn kho về 0 bằng 1 giao dịch MANUAL_BLOCK.
     */
    @Transactional
    public StockTransactionResponse applyKillSwitch(UUID itemId, String reason) {
        InventoryItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Nguyên liệu không tồn tại"));

        java.util.List<InventoryLevel> levels = levelRepository.findByItemId(itemId);
        if (levels.isEmpty()) {
            throw new ResourceNotFoundException("Tồn kho chưa được khởi tạo");
        }

        BigDecimal currentStock = levels.stream()
                .map(InventoryLevel::getCurrentStock)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (currentStock.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Kill-switch: {} đã hết hàng (tồn = {}), chỉ fire event.", item.getName(), currentStock);
            eventPublisher.publishEvent(
                    new com.fnb.inventory.dto.event.InventoryOutOfStockEvent(item.getId(), item.getName()));
            StockTransaction dummy = StockTransaction.builder()
                    .item(item)
                    .transactionType(TransactionType.MANUAL_BLOCK)
                    .quantityChange(BigDecimal.ZERO)
                    .reason(reason + " (đã hết từ trước)")
                    .build();
            return mapToResponse(transactionRepository.save(dummy));
        }

        BigDecimal deduction = currentStock.negate();
        StockTransactionRequest req = StockTransactionRequest.builder()
                .itemId(itemId)
                .transactionType(TransactionType.MANUAL_BLOCK)
                .quantityChange(deduction)
                .reason(reason)
                .build();

        log.warn("Kill-switch: {} ({} → 0) - Lý do: {}", item.getName(), currentStock, reason);
        return recordTransaction(req);
    }

    """
    
    new_content = content[:start_idx] + new_methods + content[end_idx:]
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(new_content)
    print("Successfully replaced StockTransactionService methods!")
else:
    print("Could not find start or end index.")
