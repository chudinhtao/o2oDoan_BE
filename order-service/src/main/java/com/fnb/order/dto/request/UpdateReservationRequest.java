package com.fnb.order.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class UpdateReservationRequest {
    private String customerName;
    private String customerPhone;
    private Integer partySize;
    private Integer adultCount;
    private Integer childrenCount;
    private LocalDateTime bookingTime;
    private String note;
    private String preOrderDraft;
    private BigDecimal depositAmount;
}
