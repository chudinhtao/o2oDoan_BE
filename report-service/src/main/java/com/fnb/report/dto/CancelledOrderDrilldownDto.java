package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Chi tiết đơn hàng bị hủy theo lý do / khu vực.
 * Dùng cho tool getOperationalDrilldown (Giai đoạn 1.4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelledOrderDrilldownDto {
    private String cancellationReason; // Lý do hủy (nếu có)
    private long cancelCount;
    private BigDecimal cancelledRevenue;  // Doanh thu bị mất
    private double cancelRate;            // % đơn bị hủy trên tổng đơn
}
