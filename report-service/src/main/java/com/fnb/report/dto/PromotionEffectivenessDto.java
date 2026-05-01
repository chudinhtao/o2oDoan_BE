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
public class PromotionEffectivenessDto {
    private String promotionCode;
    private long orderCount;
    private BigDecimal totalDiscountGiven;
    private BigDecimal grossRevenue;
    private BigDecimal avgOrderValue;
}
