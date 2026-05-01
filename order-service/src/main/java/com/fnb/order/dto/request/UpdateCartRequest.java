package com.fnb.order.dto.request;

import lombok.Data;
import jakarta.validation.constraints.Min;

@Data
public class UpdateCartRequest {
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private int quantity;
    private String note;
}
