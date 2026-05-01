package com.fnb.kds.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KdsOrderPaidEvent {
    private UUID orderId;
    private UUID tableId;
    private Integer tableNumber;
    private String sessionToken;
}
