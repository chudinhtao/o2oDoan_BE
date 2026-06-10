package com.fnb.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
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
public class StocktakeItemUpdateRequest {
    @NotNull(message = "Stocktake Item ID is required")
    private UUID id;

    @NotNull(message = "Counted quantity is required")
    private BigDecimal countedQuantity;

    private String adjustmentReason;
}
