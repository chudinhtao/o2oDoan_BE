package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_ticket_items", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTicketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private OrderTicket ticket;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(length = 20)
    @Builder.Default
    private String status = "PENDING"; 
    // PENDING, PREPARING, DONE, SERVED, CANCELLED, RETURNED

    @Column(length = 20, nullable = false)
    private String station; // HOT, COLD, DRINK

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Phase 1 — Staff Tracking: Ai là người bưng món ra bàn? */
    @Column(name = "served_by")
    private UUID servedBy;

    /** Phase 1 — Kitchen KPI: Món này được làm xong lúc mấy giờ? */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Phase 2 — Server Role: Thời điểm bưng món (dùng cho Undo 30s và KPI). */
    @Column(name = "served_at")
    private LocalDateTime servedAt;

    /** Phase 1 — Fraud Prevention: Ai là người duyệt hủy món này? */
    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @OneToMany(mappedBy = "ticketItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItemOption> options = new ArrayList<>();
}
