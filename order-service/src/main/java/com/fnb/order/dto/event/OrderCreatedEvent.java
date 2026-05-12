package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
    private UUID orderId;
    private UUID ticketId;
    private Integer tableNumber;
    private String sessionToken;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderCreatedItemEvent> items;
    /** "DINE_IN", "TAKEAWAY", "DELIVERY" — để Frontend phân loại mà không cần gọi thêm API */
    private String orderType;
    /** VD: "Bàn 12" hoặc "Mang Đi #001a" */
    private String orderIdentifier;
}
