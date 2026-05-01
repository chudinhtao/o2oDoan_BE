package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketUpdatedEvent {
    private UUID ticketId;
    private UUID itemId;
    private String sessionToken;
    private String status;
    private Integer tableNumber;
    private String type; // TICKET hoặc ITEM
    private LocalDateTime updatedAt;
}
