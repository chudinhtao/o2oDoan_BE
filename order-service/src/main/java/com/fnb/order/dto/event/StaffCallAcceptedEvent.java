package com.fnb.order.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Event bắn khi một Server tiếp nhận (accept) yêu cầu gọi phục vụ.
 * Mục đích: Notification Service đẩy xuống WebSocket để các Server
 * khác tự động lock nút "Tiếp nhận" của yêu cầu đó.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCallAcceptedEvent {
    private UUID callId;
    private UUID acceptedBy;
    private String acceptedByName; // Tên hiển thị cho FE
    private Integer tableNumber;
    private String callType;
    private LocalDateTime acceptedAt;
}
