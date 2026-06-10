package com.fnb.inventory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class UomConversionResponse {
    private UUID id;
    private UUID itemId;
    private String itemName;
    private UomResponse fromUom;
    private UomResponse toUom;
    private BigDecimal conversionRate;
}
