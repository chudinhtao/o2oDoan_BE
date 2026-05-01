package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftReportDto {
    private LocalDate shiftDate;
    private BigDecimal totalRevenue;
    private long totalOrders;
    private Map<String, BigDecimal> revenueByPaymentMethod;
    private Map<String, Long> ordersByPaymentMethod;
    private long cancelledOrders;
    private BigDecimal cancelledRevenue;
}
