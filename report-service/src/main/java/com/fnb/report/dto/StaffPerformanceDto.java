package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffPerformanceDto {
    private UUID cashierId;
    private String staffName;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private Long cancelledOrders;
    private BigDecimal cancelledRevenue;
    private Double cancelRate; // Percentage of orders cancelled
}
