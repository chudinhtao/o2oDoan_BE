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
public class DailyReservationTrendDto {
    private String day;
    private long totalReservations;
    private long totalCompleted;
    private long totalCancelled;
}
