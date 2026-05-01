package com.fnb.order.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class TicketItemRequest {

    @NotNull(message = "Thiếu định danh món ăn")
    private UUID menuItemId;

    @Min(value = 1, message = "Số lượng ít nhất là 1")
    private int quantity;

    private String note;

    private List<TicketItemOptionRequest> options;
}
