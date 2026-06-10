package com.fnb.auth.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkScheduleDto {
    private UUID id;
    private UUID userId;
    private String fullName;
    private UUID shiftId;
    private String shiftName;
    private LocalDate workDate;
    private String status;
    private String notes;
}
