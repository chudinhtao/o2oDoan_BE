package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TableResponse {
    private UUID id;
    private Integer number;
    private String name;
    private String qrUrl;
    private String status;
    private Integer capacity;
    private boolean isActive;
    private String zone;
}
