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
public class StaffTimesheetDto {
    private UUID staffId;
    private String staffName;
    private String role;
    private int totalShifts;
    private double totalWorkingHours;
    private BigDecimal totalRevenue;      // Chi dung cho CASHIER
    private BigDecimal revenuePerHour;    // Chi dung cho CASHIER
    private long itemsPrepared;           // Chi dung cho KITCHEN
    private long callsResolved;           // Chi dung cho SERVER
}
