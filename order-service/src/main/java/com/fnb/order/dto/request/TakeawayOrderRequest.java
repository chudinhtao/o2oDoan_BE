package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class TakeawayOrderRequest {

    private String note;
    private String promotionCode;

    @NotEmpty(message = "Danh sách món không được để trống")
    private List<TicketItemRequest> items;
}
