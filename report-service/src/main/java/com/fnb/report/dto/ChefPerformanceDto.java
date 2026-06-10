package com.fnb.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChefPerformanceDto {
    private UUID chefId;
    private String chefName;
    private int totalItemsPrepared;
    private double avgPrepMinutes;
    private int lateItemCount;
    private double lateRate;
}
