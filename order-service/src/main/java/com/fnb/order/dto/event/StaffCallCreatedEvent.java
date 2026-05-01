package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StaffCallCreatedEvent {
    private UUID callId;
    private UUID sessionId;
    private UUID tableId;
    private Integer tableNumber;
    private String callType;
    private LocalDateTime calledAt;
}
