import re

file_path = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\OrderEventConsumerService.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Update handleOrderPaid to handleOrderCreatedOrPaid
content = re.sub(
    r'@KafkaListener\(topics = "order.paid", groupId = "\$\{spring\.kafka\.consumer\.group-id\}"\)\s*@Transactional\s*public void handleOrderPaid\(String message\) \{',
    r'''@KafkaListener(topics = {"order.created", "order.paid"}, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCreatedOrPaid(String message) {''',
    content
)

content = content.replace('log.info("Received order.paid event for order: {}", orderId);', 'log.info("Received order.created/paid event for order: {}", orderId);')
content = content.replace('log.error("Error processing order.paid event: {}", e.getMessage(), e);', 'log.error("Error processing order.created/paid event: {}", e.getMessage(), e);')

# 2. Update handleOrderCancelled to parse kitchenStatus
new_cancel_logic = '''
            String kitchenStatus = "PENDING";
            if (event.has("kitchenStatus")) {
                kitchenStatus = event.get("kitchenStatus").asText();
            } else if (event.has("status")) {
                kitchenStatus = event.get("status").asText();
            }
            boolean isWaste = "PREPARING".equalsIgnoreCase(kitchenStatus) || "DONE".equalsIgnoreCase(kitchenStatus) || "SERVED".equalsIgnoreCase(kitchenStatus);
            
            if (event.has("lineItems")) {
                for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    processItemCancel(orderId, orderLineItemId, menuItemId, quantity, isWaste);
                }
            }
'''

# Use regex to replace the inside of handleOrderCancelled's if(event.has("lineItems")) loop
content = re.sub(
    r'if \(event\.has\("lineItems"\)\) \{\s*for \(JsonNode item : event\.get\("lineItems"\)\) \{\s*UUID orderLineItemId.*?\s*UUID menuItemId.*?\s*int quantity.*?\s*processItemRefund\(orderId, orderLineItemId, menuItemId, quantity\);\s*\}\s*\}',
    new_cancel_logic.strip(),
    content,
    flags=re.DOTALL
)

# 3. Update processItemDeduction to include wastage
old_deduction = 'BigDecimal totalDeduction = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));'
new_deduction = '''BigDecimal baseQty = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));
            BigDecimal wastage = recipeItem.getWastagePercent() != null ? recipeItem.getWastagePercent() : BigDecimal.ZERO;
            BigDecimal totalDeduction = baseQty.multiply(BigDecimal.ONE.add(wastage.divide(BigDecimal.valueOf(100))));'''

content = content.replace(old_deduction, new_deduction)

# 4. Rename processItemRefund to processItemCancel and add isWaste parameter
content = content.replace('private void processItemRefund(UUID orderId, UUID orderLineItemId, UUID menuItemId, int quantity) {', 'private void processItemCancel(UUID orderId, UUID orderLineItemId, UUID menuItemId, int quantity, boolean isWaste) {')

# Inside processItemCancel, we use either REFUND or OUT_WASTE
old_refund_tx = '''StockTransactionRequest txRequest = StockTransactionRequest.builder()
                    .itemId(recipeItem.getInventoryItem().getId())
                    .transactionType(TransactionType.REFUND)
                    .quantityChange(totalRefund) // Positive because we are refunding
                    .referenceId(orderId)
                    .orderLineItemId(orderLineItemId)
                    .reason("Hoàn kho do hủy hóa đơn/món: " + orderId)
                    .build();'''

new_refund_tx = '''
            TransactionType type = isWaste ? TransactionType.OUT_WASTE : TransactionType.REFUND;
            BigDecimal qtyChange = isWaste ? totalRefund.negate() : totalRefund;
            String reason = isWaste ? "Hao hụt do hủy món đã chế biến: " + orderId : "Hoàn kho do hủy hóa đơn/món: " + orderId;

            // Idempotency for OUT_WASTE as well
            if (isWaste) {
                boolean alreadyWasted = stockTransactionRepository.existsByReferenceIdAndOrderLineItemIdAndItem_IdAndTransactionType(
                        orderId, orderLineItemId, recipeItem.getInventoryItem().getId(), TransactionType.OUT_WASTE);
                if (alreadyWasted) continue;
            }

            StockTransactionRequest txRequest = StockTransactionRequest.builder()
                    .itemId(recipeItem.getInventoryItem().getId())
                    .transactionType(type)
                    .quantityChange(qtyChange) 
                    .referenceId(orderId)
                    .orderLineItemId(orderLineItemId)
                    .reason(reason)
                    .build();'''

content = content.replace(old_refund_tx, new_refund_tx)

# Also update the wastage calculation in processItemCancel
content = content.replace('BigDecimal totalRefund = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));', '''BigDecimal baseQty = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));
            BigDecimal wastage = recipeItem.getWastagePercent() != null ? recipeItem.getWastagePercent() : BigDecimal.ZERO;
            BigDecimal totalRefund = baseQty.multiply(BigDecimal.ONE.add(wastage.divide(BigDecimal.valueOf(100))));''')

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Successfully refactored OrderEventConsumerService!")
