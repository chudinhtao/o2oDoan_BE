package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderCreatedOptionEvent {
    private String optionName;
    private BigDecimal extraPrice;
}
