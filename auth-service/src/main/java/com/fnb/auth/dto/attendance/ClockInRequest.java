package com.fnb.auth.dto.attendance;

import lombok.Data;

import java.util.UUID;

@Data
public class ClockInRequest {
    private String note;
    private UUID scheduleId; // Optional, mapping to WorkSchedule if strict
}
