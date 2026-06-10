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
public class ReservationReportDto {
    private long totalReservations;
    private long totalCompleted;
    private long totalCancelled;
    private long totalNoShow;
    private BigDecimal totalDeposits;
    private BigDecimal pendingRefund;
    private BigDecimal refunded;
    private BigDecimal forfeited;
    private java.util.List<DailyReservationTrendDto> dailyTrend;
}
