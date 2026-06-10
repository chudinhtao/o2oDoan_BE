package com.fnb.auth.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceLogDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private UUID scheduleId;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private String checkInNote;
    private String checkOutNote;
    private boolean late;
    private boolean earlyLeave;
    private LocalDateTime createdAt;
}
