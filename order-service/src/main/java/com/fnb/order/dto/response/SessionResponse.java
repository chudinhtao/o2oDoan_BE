package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class SessionResponse {
    private UUID id;
    private UUID tableId;
    private Integer tableNumber;
    private String sessionToken;
    private String status;
    private LocalDateTime openedAt;
    private LocalDateTime expiresAt;
}
