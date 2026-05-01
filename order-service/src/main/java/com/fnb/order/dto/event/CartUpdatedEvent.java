package com.fnb.order.dto.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CartUpdatedEvent {
    private String sessionToken;
    private Integer tableNumber;
}
