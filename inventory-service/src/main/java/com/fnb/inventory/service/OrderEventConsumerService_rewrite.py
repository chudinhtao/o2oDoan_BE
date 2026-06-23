import re

file_path = "d:\\srcDOAN\\backend\\inventory-service\\src\\main\\java\\com\\fnb\\inventory\\service\\OrderEventConsumerService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

old_execute_cancel = """        for (Map.Entry<UUID, CancelAggregate> entry : cancelMap.entrySet()) {
            UUID itemId = entry.getKey();
            CancelAggregate agg = entry.getValue();

            if (agg.isWaste()) {
                // 1. Reverse OUT_SALE
                stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                        .itemId(itemId)
                        .transactionType(TransactionType.REFUND)
                        .quantityChange(agg.getAmount())
                        .locationId(agg.getLocationId())
                        .referenceId(orderId)
                        .orderLineItemId(orderLineItemId)
                        .reason("Reversing sale for waste record: " + orderId)
                        .build());

                // 2. Record OUT_WASTE
                stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                        .itemId(itemId)
                        .transactionType(TransactionType.OUT_WASTE)
                        .quantityChange(agg.getAmount().negate())
                        .locationId(agg.getLocationId())
                        .referenceId(orderId)
                        .orderLineItemId(orderLineItemId)
                        .reason("Waste due to cancellation of prepared item: " + orderId)
                        .build());
            } else {
                stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                        .itemId(itemId)
                        .transactionType(TransactionType.REFUND)
                        .quantityChange(agg.getAmount())
                        .locationId(agg.getLocationId())
                        .referenceId(orderId)
                        .orderLineItemId(orderLineItemId)
                        .reason("Stock refund due to order/item cancellation: " + orderId)
                        .build());
            }
        }"""

new_execute_cancel = """        for (Map.Entry<UUID, CancelAggregate> entry : cancelMap.entrySet()) {
            UUID itemId = entry.getKey();
            CancelAggregate agg = entry.getValue();

            java.util.List<com.fnb.inventory.entity.StockTransaction> historicalOutSales = stockTransactionRepository.findByOrderLineItemIdAndItemIdAndTransactionType(
                    orderLineItemId, itemId, TransactionType.OUT_SALE);

            if (historicalOutSales == null || historicalOutSales.isEmpty()) {
                if (agg.isWaste()) {
                    // 1. Reverse OUT_SALE
                    stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                            .itemId(itemId)
                            .transactionType(TransactionType.REFUND)
                            .quantityChange(agg.getAmount())
                            .locationId(agg.getLocationId())
                            .referenceId(orderId)
                            .orderLineItemId(orderLineItemId)
                            .reason("Reversing sale for waste record: " + orderId)
                            .build());

                    // 2. Record OUT_WASTE
                    stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                            .itemId(itemId)
                            .transactionType(TransactionType.OUT_WASTE)
                            .quantityChange(agg.getAmount().negate())
                            .locationId(agg.getLocationId())
                            .referenceId(orderId)
                            .orderLineItemId(orderLineItemId)
                            .reason("Waste due to cancellation of prepared item: " + orderId)
                            .build());
                } else {
                    stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                            .itemId(itemId)
                            .transactionType(TransactionType.REFUND)
                            .quantityChange(agg.getAmount())
                            .locationId(agg.getLocationId())
                            .referenceId(orderId)
                            .orderLineItemId(orderLineItemId)
                            .reason("Stock refund due to order/item cancellation: " + orderId)
                            .build());
                }
            } else {
                for (com.fnb.inventory.entity.StockTransaction outSale : historicalOutSales) {
                    java.math.BigDecimal qtyToRefund = outSale.getQuantityChange().abs();
                    String lotNumber = outSale.getBatch() != null ? outSale.getBatch().getLotNumber() : null;
                    UUID locationId = outSale.getLocation() != null ? outSale.getLocation().getId() : agg.getLocationId();

                    if (agg.isWaste()) {
                        // 1. Reverse OUT_SALE with exact batch
                        stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                                .itemId(itemId)
                                .transactionType(TransactionType.REFUND)
                                .quantityChange(qtyToRefund)
                                .locationId(locationId)
                                .referenceId(orderId)
                                .orderLineItemId(orderLineItemId)
                                .lotNumber(lotNumber)
                                .reason("Reversing sale for waste record: " + orderId)
                                .build());

                        // 2. Record OUT_WASTE with exact batch
                        stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                                .itemId(itemId)
                                .transactionType(TransactionType.OUT_WASTE)
                                .quantityChange(qtyToRefund.negate())
                                .locationId(locationId)
                                .referenceId(orderId)
                                .orderLineItemId(orderLineItemId)
                                .lotNumber(lotNumber)
                                .reason("Waste due to cancellation of prepared item: " + orderId)
                                .build());
                    } else {
                        // 1. Reverse OUT_SALE with exact batch
                        stockTransactionService.recordTransaction(StockTransactionRequest.builder()
                                .itemId(itemId)
                                .transactionType(TransactionType.REFUND)
                                .quantityChange(qtyToRefund)
                                .locationId(locationId)
                                .referenceId(orderId)
                                .orderLineItemId(orderLineItemId)
                                .lotNumber(lotNumber)
                                .reason("Stock refund due to order/item cancellation: " + orderId)
                                .build());
                    }
                }
            }
        }"""

if old_execute_cancel in content:
    content = content.replace(old_execute_cancel, new_execute_cancel)
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    print("executeCancel successfully replaced.")
else:
    print("Could not find old_execute_cancel block.")
