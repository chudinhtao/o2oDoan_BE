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
public class InventorySuggestionResponse {
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private BigDecimal currentStock;
    private BigDecimal safetyStock;
    private BigDecimal suggestedQuantity;
    private UUID supplierId;
    private String supplierName;
    private String uomName;
}
