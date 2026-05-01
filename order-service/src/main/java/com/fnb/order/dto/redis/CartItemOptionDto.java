package com.fnb.order.dto.redis;

import lombok.Data;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
public class CartItemOptionDto {
    private UUID optionId;
    private String optionName;
    private BigDecimal extraPrice;
}
