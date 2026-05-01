package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Thống kê hiệu suất bếp: thời gian làm món, số ticket trễ.
 * Dùng cho tool getOperationalDrilldown (Giai đoạn 1.4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KitchenPerformanceDto {
    private String itemName;
    private long totalTickets;
    private BigDecimal avgPrepMinutes;   // Thời gian làm món TB (phút)
    private long lateTickets;            // Số ticket bị trễ (>15 phút)
    private double lateRate;             // Tỷ lệ trễ (%)
}
