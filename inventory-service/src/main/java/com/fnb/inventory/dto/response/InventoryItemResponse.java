package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class InventoryItemResponse {
    private UUID id;
    private String sku;
    private String name;
    private com.fnb.inventory.enums.ItemType type;
    private BigDecimal safetyStock;
    private BigDecimal avgCostPrice;
    private BigDecimal currentStock;
    private boolean isActive;

    // Nested references
    private ItemCategoryResponse category;
    private UomResponse baseUom;
    
    private java.util.List<InventoryBatchResponse> batches;
}
