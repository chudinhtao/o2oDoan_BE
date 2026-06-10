package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class OrderCreatedOptionEvent {
    private UUID menuItemId;
    private String optionName;
    private BigDecimal extraPrice;
}
