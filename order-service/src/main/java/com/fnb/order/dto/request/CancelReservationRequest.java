package com.fnb.order.dto.request;

import lombok.Data;

@Data
public class CancelReservationRequest {
    private String reason;
    private String status; // CANCELLED or NO_SHOW
    private String refundStatus;
}
