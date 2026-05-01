package com.fnb.order.dto.event;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableStatusUpdatedEvent {
    private UUID tableId;
    private String status; // FREE, OCCUPIED, CLEANING
    private String sessionToken;
}
