package com.fnb.kds.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KdsOrderCreatedOptionEvent {
    private String optionName;
    private BigDecimal extraPrice;
}
