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
public class InventoryVarianceDto {
    private UUID ingredientId;
    private String ingredientName;
    private String uomName;
    private BigDecimal theoreticalUsage;
    private BigDecimal actualUsage;
    private BigDecimal variance;
    private BigDecimal varianceValue;
}
