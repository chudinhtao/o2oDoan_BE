package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServerPerformanceDto {
    private UUID serverId;
    private String serverName;
    private int totalCallsResolved;
    private double avgResponseSeconds;
    private double avgResolutionMinutes;
    private int totalItemsServed;
    private double avgDeliverySeconds;
}
