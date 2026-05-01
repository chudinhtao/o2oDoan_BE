package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderSummaryItemResponse {
    private UUID menuItemId;
    private String itemName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal priceTotal;
    private String note;
    private List<OrderItemOptionResponse> options;
}
