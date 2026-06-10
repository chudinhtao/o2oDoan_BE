package com.fnb.order.dto.request;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReservationRequest {
    private String customerName;
    private String customerPhone;
    private Integer partySize;
    private Integer adultCount;
    private Integer childrenCount;
    private LocalDateTime bookingTime;
    private String preOrderDraft; // JSON string
    private String note;
    private BigDecimal depositAmount;
}
