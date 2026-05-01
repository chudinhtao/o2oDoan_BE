package com.fnb.order.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTableResponse {
    private UUID id;
    private Integer number;
    private String name;
    private String status;
    private Integer capacity;
    private UUID currentSessionId;
    private String currentSessionToken;
    private BigDecimal totalAmount;
    private LocalDateTime openedAt;
    private UUID parentTableId;
    private Integer parentTableNumber;
}
