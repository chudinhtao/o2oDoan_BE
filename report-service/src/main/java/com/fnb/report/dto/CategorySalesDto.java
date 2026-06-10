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
public class CategorySalesDto {
    private UUID categoryId;
    private String categoryName;
    private int totalQuantitySold;
    private BigDecimal totalRevenue; // Gross
    private BigDecimal totalTax;
    private BigDecimal totalNetRevenue;
    private double revenuePercentage;
}
