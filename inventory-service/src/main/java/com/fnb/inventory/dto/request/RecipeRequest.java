package com.fnb.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class RecipeRequest {

    private UUID saleItemId;  // for MAIN_ITEM
    private UUID modifierId;  // for MODIFIER

    @NotNull(message = "Loại công thức không được để trống (MAIN_ITEM hoặc MODIFIER)")
    private com.fnb.inventory.enums.RecipeType type; // MAIN_ITEM, MODIFIER

    private UUID defaultLocationId;

    @NotEmpty(message = "Công thức phải có ít nhất 1 nguyên liệu")
    @Valid
    private List<RecipeItemRequest> items;

    @Data
    public static class RecipeItemRequest {
        @NotNull(message = "ID nguyên liệu không được để trống")
        private UUID inventoryItemId;

        @NotNull(message = "Định lượng không được để trống")
        private BigDecimal quantity;

        @NotNull(message = "Đơn vị tính không được để trống")
        private UUID uomId;

        private BigDecimal wastagePercent;

        private com.fnb.inventory.enums.IngredientScope scope;
    }
}
