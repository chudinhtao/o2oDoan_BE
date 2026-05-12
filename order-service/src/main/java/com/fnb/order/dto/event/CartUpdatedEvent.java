package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartUpdatedEvent {
    private String sessionToken;
    private Integer tableNumber;
    /** "DINE_IN" hoặc "TAKEAWAY" — Frontend phân biệt tab */
    private String orderType;
}
