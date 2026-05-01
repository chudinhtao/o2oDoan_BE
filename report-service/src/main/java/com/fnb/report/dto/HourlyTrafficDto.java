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
public class HourlyTrafficDto {
    private int hourOfDay;
    private long orderCount;
    private BigDecimal revenue;
    private BigDecimal avgOrderValue;
}
