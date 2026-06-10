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
public class ProfitLossDto {
    private LocalDate startDate;
    private LocalDate endDate;
    
    private BigDecimal totalRevenue;
    private BigDecimal totalTax;
    private BigDecimal totalCogs;
    private BigDecimal totalWaste;
    
    private BigDecimal grossProfit; // Revenue - COGS
    private BigDecimal netProfit;   // Gross - Waste
    
    private BigDecimal profitMargin; // (Net Profit / Revenue) * 100
}
