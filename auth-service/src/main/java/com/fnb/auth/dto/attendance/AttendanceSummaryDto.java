package com.fnb.auth.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSummaryDto {
    private UUID userId;
    private String fullName;
    private int totalShifts;
    private double totalWorkingHours;
    private int totalLateMinutes;
    private int totalEarlyLeaveMinutes;
    private int totalOtMinutes;
}
