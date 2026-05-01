package com.fnb.kds.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KdsOrderCreatedEvent {
    private UUID orderId;
    private UUID ticketId;
    private Integer tableNumber;
    private String sessionToken;
    private String note;
    private LocalDateTime createdAt;
    private List<KdsOrderCreatedItemEvent> items;
}
