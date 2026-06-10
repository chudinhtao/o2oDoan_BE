package com.fnb.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VarianceReportResponse {
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal totalEstimatedLossValue;
    private List<VarianceReportItemResponse> items;
}
