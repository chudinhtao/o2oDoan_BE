package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffCallRequest {
    @NotBlank(message = "Loại yêu cầu không được để trống")
    private String callType; // WATER, BILL, CLEAN, SUPPORT
}
