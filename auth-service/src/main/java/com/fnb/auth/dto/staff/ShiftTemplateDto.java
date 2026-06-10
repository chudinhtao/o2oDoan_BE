package com.fnb.auth.dto.staff;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShiftTemplateDto {
    private UUID id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String colorCode;
    private boolean active;
}
