import re

file_path = "d:\\srcDOAN\\backend\\inventory-service\\src\\main\\java\\com\\fnb\\inventory\\service\\StockTransactionService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Fix 1: The REFUND logic inside `> 0` block
old_refund_logic = """                targetLevel = null;

                if (TransactionType.REFUND.equals(request.getTransactionType())) {
                    // [BUGFIX] Hoàn trả đơn hàng (REFUND): Tự động trả về Lô đang sử dụng (FEFO)
                    targetLevel = levels.stream()
                            .filter(l -> {
                                if (finalLoc == null) return l.getLocation() == null;
                                return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                            })
                            .filter(l -> l.getBatch() != null && l.getBatch().getExpiryDate() != null)
                            .min(java.util.Comparator.comparing(l -> l.getBatch().getExpiryDate()))
                            .orElse(null);

                    // NẾU KHÔNG CÓ LÔ NÀO KHẢ DỤNG, TẠO LÔ MỚI MANG TÊN RETURN-[Mã đơn hàng]
                    if (targetLevel == null && request.getReferenceId() != null) {
                        String returnLotNumber = "RETURN-" + request.getReferenceId().toString().substring(0, 6).toUpperCase();
                        
                        com.fnb.inventory.entity.InventoryBatch returnBatch = batchRepository.findFirstByItemIdAndLotNumber(item.getId(), returnLotNumber)
                            .orElseGet(() -> {
                                com.fnb.inventory.entity.InventoryBatch newBatch = com.fnb.inventory.entity.InventoryBatch.builder()
                                    .item(item)
                                    .lotNumber(returnLotNumber)
                                    .build();
                                return batchRepository.save(newBatch);
                            });

                        targetLevel = levels.stream()
                            .filter(l -> {
                                if (finalLoc == null) return l.getLocation() == null;
                                return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                            })
                            .filter(l -> l.getBatch() != null && l.getBatch().getId().equals(returnBatch.getId()))
                            .findFirst()
                            .orElse(null);

                        if (targetLevel == null) {
                            targetLevel = com.fnb.inventory.entity.InventoryLevel.builder()
                                    .item(item)
                                    .batch(returnBatch)
                                    .location(finalLoc)
                                    .currentStock(BigDecimal.ZERO)
                                    .build();
                            targetLevel = levelRepository.save(targetLevel);
                            levels.add(targetLevel);
                        }

                        // Cập nhật lại lô cho giao dịch này
                        transaction.setBatch(returnBatch);
                        transaction = transactionRepository.save(transaction);
                    }
                }"""

new_refund_logic = """                targetLevel = null;

                if (TransactionType.REFUND.equals(request.getTransactionType()) || TransactionType.ADJUSTMENT.equals(request.getTransactionType())) {
                    if ("N/A".equals(request.getLotNumber()) || (TransactionType.ADJUSTMENT.equals(request.getTransactionType()) && request.getLotNumber() == null)) {
                        targetLevel = levels.stream()
                                .filter(l -> {
                                    if (finalLoc == null) return l.getLocation() == null;
                                    return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                                })
                                .filter(l -> l.getBatch() == null)
                                .findFirst()
                                .orElse(null);
                    } else if (request.getLotNumber() != null && !request.getLotNumber().trim().isEmpty()) {
                        targetLevel = levels.stream()
                                .filter(l -> {
                                    if (finalLoc == null) return l.getLocation() == null;
                                    return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                                })
                                .filter(l -> l.getBatch() != null && l.getBatch().getLotNumber().equals(request.getLotNumber()))
                                .findFirst()
                                .orElse(null);
                    } else {
                        // Logic cũ: tìm lô FEFO (chưa đầy / phù hợp)
                        targetLevel = levels.stream()
                                .filter(l -> {
                                    if (finalLoc == null) return l.getLocation() == null;
                                    return l.getLocation() != null && l.getLocation().getId().equals(finalLoc.getId());
                                })
                                .filter(l -> l.getBatch() != null && l.getBatch().getExpiryDate() != null)
                                .min(java.util.Comparator.comparing(l -> l.getBatch().getExpiryDate()))
                                .orElse(null);
                    }
                }"""

if old_refund_logic in content:
    content = content.replace(old_refund_logic, new_refund_logic)
    print("Replaced old refund logic successfully.")
else:
    print("Could not find old refund logic.")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
