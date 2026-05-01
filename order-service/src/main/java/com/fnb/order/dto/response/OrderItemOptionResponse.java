package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderItemOptionResponse {
    private UUID id;
    private String optionName;
    private BigDecimal extraPrice;
}
