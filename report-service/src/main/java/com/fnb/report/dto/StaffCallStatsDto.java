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
public class StaffCallStatsDto {
    private String tableNumber;
    private String callType;
    private long callCount;
    private BigDecimal avgResolveMinutes;
}
