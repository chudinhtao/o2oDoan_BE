package com.fnb.order.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class StaffCallResponse {
    private UUID id;
    private UUID sessionId;
    private UUID tableId;
    private Integer tableNumber;
    private String callType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    /** Server đã tiếp nhận: dùng để FE lock nút "Tiếp nhận" của người khác. */
    private UUID acceptedBy;
    private LocalDateTime acceptedAt;
}
