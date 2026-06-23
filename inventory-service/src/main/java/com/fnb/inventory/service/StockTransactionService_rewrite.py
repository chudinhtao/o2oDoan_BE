import re

file_path = "d:\\srcDOAN\\backend\\inventory-service\\src\\main\\java\\com\\fnb\\inventory\\service\\StockTransactionService.java"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Change signature
content = content.replace("public StockTransactionResponse recordTransaction(StockTransactionRequest request) {", "public java.util.List<StockTransactionResponse> recordTransaction(StockTransactionRequest request) {")
content = content.replace("public StockTransactionResponse applyKillSwitch(UUID itemId, String reason) {", "public java.util.List<StockTransactionResponse> applyKillSwitch(UUID itemId, String reason) {")
content = content.replace("return mapToResponse(transactionRepository.save(dummy));", "return java.util.Collections.singletonList(mapToResponse(transactionRepository.save(dummy)));")

# 2. Rewrite recordTransaction body
# Find the start of recordTransaction body
pattern = r"(public java\.util\.List<StockTransactionResponse> recordTransaction\(StockTransactionRequest request\) \{)(.*?)(return mapToResponse\(transaction\);\n    \})"
match = re.search(pattern, content, re.DOTALL)

if match:
    body = match.group(2)
    # The current body creates `transaction` at the top:
    # StockTransaction transaction = StockTransaction.builder()...
    
    # We will replace the creation of the transaction to only happen for >= 0
    # Wait, it's easier to just do string replacements on the body.

    # 2.1 Add responses list
    body = body.replace("log.info(\"Recording stock transaction for item {}: {} ({})\", \n                request.getItemId(), request.getTransactionType(), request.getQuantityChange());", 
                        "log.info(\"Recording stock transaction for item {}: {} ({})\", \n                request.getItemId(), request.getTransactionType(), request.getQuantityChange());\n\n        java.util.List<StockTransactionResponse> responses = new java.util.ArrayList<>();")

    # 2.2 Remove the single transaction saving
    old_tx_block = """        StockTransaction transaction = StockTransaction.builder()
                .item(item)
                .batch(batchLookup)
                .transactionType(request.getTransactionType())
                .quantityChange(request.getQuantityChange())
                .unitPriceAtTransaction(priceAtTransaction)
                .referenceId(request.getReferenceId())
                .orderLineItemId(request.getOrderLineItemId())
                .location(request.getLocationId() != null ? locationRepository.findById(request.getLocationId()).orElse(null) : null)
                .reason(request.getReason())
                .build();
        transaction = transactionRepository.save(transaction);"""
    
    body = body.replace(old_tx_block, "")

    # 2.3 Add single transaction saving to the INBOUND / ZERO block
    old_inbound_condition = "if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {"
    new_inbound_condition = """if (remainingQty.compareTo(BigDecimal.ZERO) >= 0) {
            StockTransaction transaction = StockTransaction.builder()
                .item(item)
                .batch(batchLookup)
                .transactionType(request.getTransactionType())
                .quantityChange(request.getQuantityChange())
                .unitPriceAtTransaction(priceAtTransaction)
                .referenceId(request.getReferenceId())
                .orderLineItemId(request.getOrderLineItemId())
                .location(request.getLocationId() != null ? locationRepository.findById(request.getLocationId()).orElse(null) : null)
                .reason(request.getReason())
                .build();
            transaction = transactionRepository.save(transaction);"""
    
    body = body.replace(old_inbound_condition, new_inbound_condition)

    # 2.4 Update the fallback for < 0 block
    # It says: "} else if (remainingQty.compareTo(BigDecimal.ZERO) < 0) {"
    # We leave that as is.
    
    # 2.5 In the loop of < 0 block, add saving split transaction
    old_loop_end = """                BigDecimal deductible = lvl.getCurrentStock().min(qtyToDeduct);
                lvl.setCurrentStock(lvl.getCurrentStock().subtract(deductible));
                levelRepository.save(lvl);
                qtyToDeduct = qtyToDeduct.subtract(deductible);
            }"""
    
    new_loop_end = """                BigDecimal deductible = lvl.getCurrentStock().min(qtyToDeduct);
                lvl.setCurrentStock(lvl.getCurrentStock().subtract(deductible));
                levelRepository.save(lvl);
                qtyToDeduct = qtyToDeduct.subtract(deductible);

                StockTransaction outTx = StockTransaction.builder()
                    .item(item)
                    .batch(lvl.getBatch())
                    .transactionType(request.getTransactionType())
                    .quantityChange(deductible.negate())
                    .unitPriceAtTransaction(priceAtTransaction)
                    .referenceId(request.getReferenceId())
                    .orderLineItemId(request.getOrderLineItemId())
                    .location(lvl.getLocation())
                    .reason(request.getReason())
                    .build();
                outTx = transactionRepository.save(outTx);
                responses.add(mapToResponse(outTx));
            }"""
    body = body.replace(old_loop_end, new_loop_end)

    # 2.6 Also update the fallback negative stock creation to save transaction
    old_fallback_end = """                fallbackLevel.setCurrentStock(fallbackLevel.getCurrentStock().subtract(qtyToDeduct));
                levelRepository.save(fallbackLevel);
                log.warn("Nguyên liệu {} đã bị TRỪ ÂM KHO số lượng: {} tại location: {}", item.getName(), qtyToDeduct, request.getLocationId());
            }"""
            
    new_fallback_end = """                fallbackLevel.setCurrentStock(fallbackLevel.getCurrentStock().subtract(qtyToDeduct));
                levelRepository.save(fallbackLevel);
                log.warn("Nguyên liệu {} đã bị TRỪ ÂM KHO số lượng: {} tại location: {}", item.getName(), qtyToDeduct, request.getLocationId());

                StockTransaction fallbackTx = StockTransaction.builder()
                    .item(item)
                    .batch(null)
                    .transactionType(request.getTransactionType())
                    .quantityChange(qtyToDeduct.negate())
                    .unitPriceAtTransaction(priceAtTransaction)
                    .referenceId(request.getReferenceId())
                    .orderLineItemId(request.getOrderLineItemId())
                    .location(fallbackLevel.getLocation())
                    .reason(request.getReason())
                    .build();
                fallbackTx = transactionRepository.save(fallbackTx);
                responses.add(mapToResponse(fallbackTx));
            }"""
    body = body.replace(old_fallback_end, new_fallback_end)

    # 2.7 Replace return mapToResponse(transaction);
    content = content.replace(match.group(0), match.group(1) + body + """        if (remainingQty.compareTo(BigDecimal.ZERO) >= 0) {
            responses.add(mapToResponse(transaction));
        }
        return responses;
    }""")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Rewrite complete")
