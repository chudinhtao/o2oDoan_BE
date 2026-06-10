package com.fnb.report.dto;

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
public class TopWastedItemDto {
    private UUID ingredientId;
    private String ingredientName;
    private String uomName;
    private BigDecimal wastedQuantity;
    private BigDecimal wastedValue; // wastedQuantity * avg_cost_price
}
