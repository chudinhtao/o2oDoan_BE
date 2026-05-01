package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OrderTicketResponse {
    private UUID id;
    private UUID orderId;
    private Integer seqNumber;
    private String status;
    private String note;
    private String createdBy;
    private LocalDateTime createdAt;
    private List<OrderTicketItemResponse> items;
}
