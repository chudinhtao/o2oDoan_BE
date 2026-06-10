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
public class VarianceReportItemResponse {
    private UUID itemId;
    private String itemName;
    private String itemSku;
    private String uomName;
    private BigDecimal wasteQuantity;
    private BigDecimal adjustmentQuantity;
    private BigDecimal totalVarianceQuantity;
    private BigDecimal estimatedLossValue;
    private UUID categoryId;
    private String categoryName;
}
