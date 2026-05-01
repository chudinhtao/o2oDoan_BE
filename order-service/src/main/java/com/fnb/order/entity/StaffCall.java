package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "staff_calls", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffCall {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableInfo table;

    @Column(name = "call_type", length = 50, nullable = false)
    private String callType; // "WATER", "BILL", "CLEAN", "SUPPORT"

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; // "PENDING", "ACCEPTED", "RESOLVED"

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    /** Phase 1 — Staff KPI: Ai là nhân viên đã chạy ra bàn xử lý yêu cầu này? */
    @Column(name = "resolved_by")
    private UUID resolvedBy;

    /** Phase 2 — Server Role: Ai là người tiếp nhận yêu cầu (Optimistic Lock)? */
    @Column(name = "accepted_by")
    private UUID acceptedBy;

    /** Phase 2 — Server Role: Thời điểm tiếp nhận (dùng cho KPI độ nhạnh trả lời). */
    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(length = 200)
    private String message;
}
