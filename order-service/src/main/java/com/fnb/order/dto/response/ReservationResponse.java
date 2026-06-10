package com.fnb.order.dto.response;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ReservationResponse {
    private UUID id;
    private String customerName;
    private String customerPhone;
    private Integer partySize;
    private Integer adultCount;
    private Integer childrenCount;
    private LocalDateTime bookingTime;
    private String status;
    private BigDecimal depositAmount;
    private String refundStatus;
    private String preOrderDraft;
    private String note;
    private List<Integer> assignedTableNumbers;
}
