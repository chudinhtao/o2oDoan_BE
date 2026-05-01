package com.fnb.kds.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KdsOrderCreatedItemEvent {
    private UUID menuItemId;
    private String itemName;
    private int quantity;
    private String note;
    private String station;
    private BigDecimal unitPrice;
    private List<KdsOrderCreatedOptionEvent> options;
}
