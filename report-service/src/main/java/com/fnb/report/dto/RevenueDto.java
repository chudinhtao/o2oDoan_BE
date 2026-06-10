package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueDto {
    private LocalDate day;
    private BigDecimal revenue; // Gross
    private BigDecimal taxAmount;
    private BigDecimal netRevenue;
    private long totalOrders;
    private BigDecimal avgOrderValue;
}
