package com.fnb.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockItemResponse {
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private String uomName;
    private BigDecimal currentStock;
    private BigDecimal safetyStock;
    private BigDecimal reorderAmount;
    private UUID categoryId;
    private String categoryName;
    private BigDecimal avgCostPrice;
}
