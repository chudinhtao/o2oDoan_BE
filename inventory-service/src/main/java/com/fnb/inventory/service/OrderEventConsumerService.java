package com.fnb.inventory.service;

import com.fnb.inventory.dto.request.StockTransactionRequest;
import com.fnb.inventory.entity.Recipe;
import com.fnb.inventory.entity.RecipeItem;
import com.fnb.inventory.repository.RecipeRepository;
import com.fnb.inventory.repository.StockTransactionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fnb.inventory.enums.*;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumerService {

    private final RecipeRepository recipeRepository;
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class DeductionAggregate {
        private UUID locationId;
        private BigDecimal quantity;
    }
    
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class CancelAggregate {
        private UUID locationId;
        private BigDecimal amount;
        private boolean isWaste;
    }

    private final StockTransactionService stockTransactionService;
    private final StockTransactionRepository stockTransactionRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = {"order.created", "order.paid"}, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCreatedOrPaid(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            String orderType = event.has("orderType") ? event.get("orderType").asText() : "DINE_IN";
            log.info("Processing inventory for order: {} (Type: {})", orderId, orderType);

            JsonNode itemsNode = event.has("items") ? event.get("items") : event.get("lineItems");
            
            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    // Idempotency check for the whole line item
                    if (stockTransactionRepository.existsByOrderLineItemIdAndTransactionType(orderLineItemId, TransactionType.OUT_SALE)) {
                        log.info("Already processed inventory deduction for orderLineItemId: {}", orderLineItemId);
                        continue;
                    }

                    Map<UUID, DeductionAggregate> aggregateMap = new HashMap<>();

                    // DEDUCT stock based on recipe
                    aggregateItemDeduction(menuItemId, quantity, orderType, aggregateMap);
                    
                    // Process Toppings / Options
                    JsonNode optionsNode = item.has("options") ? item.get("options") : item.get("modifiers");
                    if (optionsNode != null && optionsNode.isArray()) {
                        for (JsonNode opt : optionsNode) {
                            if (opt.has("menuItemId")) {
                                UUID optMenuItemId = UUID.fromString(opt.get("menuItemId").asText());
                                // Toppings usually come with main item, so they share orderLineItemId
                                aggregateItemDeduction(optMenuItemId, quantity, orderType, aggregateMap);
                            }
                        }
                    }

                    // Execute deductions
                    for (Map.Entry<UUID, DeductionAggregate> entry : aggregateMap.entrySet()) {
                        StockTransactionRequest txRequest = StockTransactionRequest.builder()
                                .itemId(entry.getKey())
                                .transactionType(TransactionType.OUT_SALE)
                                .quantityChange(entry.getValue().getQuantity().negate()) // Negative because it's going out
                                .locationId(entry.getValue().getLocationId())
                                .referenceId(orderId)
                                .orderLineItemId(orderLineItemId)
                                .reason("Auto stock deduction for order: " + orderId + " (Type: " + orderType + ")")
                                .build();
                        stockTransactionService.recordTransaction(txRequest);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing order event for inventory: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "order.cancelled", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleOrderCancelled(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            log.info("Received order.cancelled event for order: {}", orderId);

            if (event.has("lineItems")) {
                for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    String kitchenStatus = item.has("kitchenStatus") ? item.get("kitchenStatus").asText() : "PENDING";
                    boolean isWaste = "PREPARING".equalsIgnoreCase(kitchenStatus) || "DONE".equalsIgnoreCase(kitchenStatus) || "SERVED".equalsIgnoreCase(kitchenStatus);
                    TransactionType targetType = isWaste ? TransactionType.OUT_WASTE : TransactionType.REFUND;

                    if (stockTransactionRepository.existsByOrderLineItemIdAndTransactionType(orderLineItemId, targetType)) {
                        log.info("Already processed {} for orderLineItemId: {}", targetType, orderLineItemId);
                        continue;
                    }

                    Map<UUID, CancelAggregate> cancelMap = new HashMap<>();

                    aggregateItemCancel(menuItemId, quantity, isWaste, cancelMap);
                    
                    if (item.has("modifiers")) {
                        for (JsonNode modifier : item.get("modifiers")) {
                            if (modifier.has("menuItemId")) {
                                UUID modMenuItemId = UUID.fromString(modifier.get("menuItemId").asText());
                                aggregateItemCancel(modMenuItemId, quantity, isWaste, cancelMap);
                            }
                        }
                    }

                    executeCancel(orderId, orderLineItemId, cancelMap);
                }
            }
        } catch (Exception e) {
            log.error("Error processing order.cancelled event: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "ticket.updated", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void handleTicketUpdated(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String status = event.get("status").asText();
            
            // Only process if status is CANCELLED or RETURNED
            if (!"CANCELLED".equalsIgnoreCase(status) && !"RETURNED".equalsIgnoreCase(status)) {
                return;
            }

            UUID orderId = UUID.fromString(event.get("orderId").asText());
            log.info("Received ticket.updated event for order: {} with status: {}", orderId, status);

            if (event.has("items") && event.get("items").isArray()) {
                for (JsonNode item : event.get("items")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();
                    
                    String kitchenStatus = item.has("kitchenStatus") ? item.get("kitchenStatus").asText() : "PENDING";
                    // If it's a "RETURNED" status, we treat it based on whether it was already served or not
                    // In most cases, if it's CANCELLED/RETURNED, we check if it's already cooked to decide on Waste
                    boolean isWaste = "PREPARING".equalsIgnoreCase(kitchenStatus) || "DONE".equalsIgnoreCase(kitchenStatus) || "SERVED".equalsIgnoreCase(kitchenStatus);
                    TransactionType targetType = isWaste ? TransactionType.OUT_WASTE : TransactionType.REFUND;

                    if (stockTransactionRepository.existsByOrderLineItemIdAndTransactionType(orderLineItemId, targetType)) {
                        log.info("Already processed {} for orderLineItemId: {}", targetType, orderLineItemId);
                        continue;
                    }
                    
                    Map<UUID, CancelAggregate> cancelMap = new HashMap<>();

                    aggregateItemCancel(menuItemId, quantity, isWaste, cancelMap);

                    if (item.has("options") && item.get("options").isArray()) {
                        for (JsonNode option : item.get("options")) {
                            if (option.has("menuItemId")) {
                                UUID optMenuItemId = UUID.fromString(option.get("menuItemId").asText());
                                aggregateItemCancel(optMenuItemId, quantity, isWaste, cancelMap);
                            }
                        }
                    }

                    executeCancel(orderId, orderLineItemId, cancelMap);
                }
            }
        } catch (Exception e) {
            log.error("Error processing ticket.updated event: {}", e.getMessage(), e);
        }
    }

    private void aggregateItemDeduction(UUID menuItemId, int quantity, String orderType, Map<UUID, DeductionAggregate> aggregateMap) {
        // Try to find recipe for MAIN_ITEM or MODIFIER
        Optional<Recipe> recipeOpt = recipeRepository.findBySaleItemIdWithItems(menuItemId);
        if (recipeOpt.isEmpty()) {
            recipeOpt = recipeRepository.findByModifierIdWithItems(menuItemId);
        }

        if (recipeOpt.isEmpty()) {
            log.warn("Menu item/Modifier {} does not have a recipe, skipping inventory deduction", menuItemId);
            return;
        }

        Recipe recipe = recipeOpt.get();
        for (RecipeItem recipeItem : recipe.getItems()) {            // Logic Dine-in vs Takeaway based on IngredientScope
            IngredientScope scope = recipeItem.getScope() != null ? recipeItem.getScope() : IngredientScope.ALWAYS;
            
            // Normalize orderType to handle potential variations like TAKE_AWAY
            String normalizedType = orderType != null ? orderType.replace("_", "").toUpperCase() : "DINEIN";

            if (scope == IngredientScope.TAKEAWAY_ONLY && !normalizedType.contains("TAKEAWAY") && !normalizedType.contains("DELIVERY")) {
                log.info("Skipping TAKEAWAY_ONLY item {} for order type {}", recipeItem.getInventoryItem().getName(), orderType);
                continue;
            }
            
            if (scope == IngredientScope.DINE_IN_ONLY && !normalizedType.contains("DINEIN")) {
                log.info("Skipping DINE_IN_ONLY item {} for order type {}", recipeItem.getInventoryItem().getName(), orderType);
                continue;
            }

            BigDecimal baseQty = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));
            BigDecimal wastage = recipeItem.getWastagePercent() != null ? recipeItem.getWastagePercent() : BigDecimal.ZERO;
            BigDecimal totalDeduction = baseQty.multiply(BigDecimal.ONE.add(wastage.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));

            UUID invItemId = recipeItem.getInventoryItem().getId();
            UUID locId = recipe.getDefaultLocation() != null ? recipe.getDefaultLocation().getId() : null;

            aggregateMap.compute(invItemId, (k, v) -> {
                if (v == null) {
                    return new DeductionAggregate(locId, totalDeduction);
                }
                v.setQuantity(v.getQuantity().add(totalDeduction));
                return v;
            });
        }
    }

    private void aggregateItemCancel(UUID menuItemId, int quantity, boolean isWaste, Map<UUID, CancelAggregate> cancelMap) {
        // Try to find recipe for MAIN_ITEM or MODIFIER
        Optional<Recipe> recipeOpt = recipeRepository.findBySaleItemIdWithItems(menuItemId);
        if (recipeOpt.isEmpty()) {
            recipeOpt = recipeRepository.findByModifierIdWithItems(menuItemId);
        }

        if (recipeOpt.isEmpty()) {
            return;
        }

        Recipe recipe = recipeOpt.get();
        for (RecipeItem recipeItem : recipe.getItems()) {
            BigDecimal baseQty = recipeItem.getQuantity().multiply(BigDecimal.valueOf(quantity));
            BigDecimal wastage = recipeItem.getWastagePercent() != null ? recipeItem.getWastagePercent() : BigDecimal.ZERO;
            BigDecimal amount = baseQty.multiply(BigDecimal.ONE.add(wastage.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)));

            UUID invItemId = recipeItem.getInventoryItem().getId();
            UUID locId = recipe.getDefaultLocation() != null ? recipe.getDefaultLocation().getId() : null;

            cancelMap.compute(invItemId, (k, v) -> {
                if (v == null) {
                    return new CancelAggregate(locId, amount, isWaste);
                }
                v.setAmount(v.getAmount().add(amount));
                return v;
            });
        }
    }

    private void executeCancel(UUID orderId, UUID orderLineItemId, Map<UUID, CancelAggregate> cancelMap) {
        // Verify if we actually deducted it before refunding
        boolean wasDeducted = stockTransactionRepository.existsByOrderLineItemIdAndTransactionType(
                orderLineItemId, TransactionType.OUT_SALE);
        
        if (!wasDeducted) {
            log.info("No OUT_SALE found for orderLineItemId: {}, skipping refund/waste record", orderLineItemId);
            return;
        }

        for (Map.Entry<UUID, CancelAggregate> entry : cancelMap.entrySet()) {
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
        }
    }
}
