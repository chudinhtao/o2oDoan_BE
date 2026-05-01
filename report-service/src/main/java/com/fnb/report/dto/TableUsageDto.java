package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableUsageDto {
    private String tableNumber;
    private String tableName;
    private String zone;
    private Integer capacity;
    private long sessionsCount;
    private BigDecimal totalRevenue;
    private BigDecimal avgSessionMinutes;
}
