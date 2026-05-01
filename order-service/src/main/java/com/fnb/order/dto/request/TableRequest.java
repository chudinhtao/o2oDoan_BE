package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TableRequest {
    @NotNull(message = "Số bàn không được trống")
    private Integer number;
    
    private String name;
    
    private Integer capacity = 4;

    private String zone;
}
