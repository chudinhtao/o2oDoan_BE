package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class RecipeResponse {
    private UUID id;
    private UUID saleItemId;
    private UUID modifierId;
    private com.fnb.inventory.enums.RecipeType type;
    private UUID defaultLocationId;
    private String defaultLocationName;
    private List<RecipeItemResponse> items;

    @Data
    @Builder
    public static class RecipeItemResponse {
        private UUID id;
        private UUID inventoryItemId;
        private String inventoryItemName;
        private BigDecimal quantity;
        private UomResponse uom;
        private BigDecimal wastagePercent;
        private com.fnb.inventory.enums.IngredientScope scope;
    }
}
