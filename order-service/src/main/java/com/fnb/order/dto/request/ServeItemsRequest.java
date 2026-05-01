package com.fnb.order.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/** Body cho API PUT /api/orders/server/deliveries/serve */
@Data
public class ServeItemsRequest {
    @NotEmpty(message = "Danh sách món không được rỗng")
    private List<UUID> itemIds;
}
