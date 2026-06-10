package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hibernate.type.SqlTypes.JSON;

@Entity
@Table(name = "reservations", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 20, nullable = false)
    private String customerPhone;

    @Column(name = "party_size", nullable = false)
    private Integer partySize;

    @Column(name = "adult_count")
    private Integer adultCount;

    @Column(name = "children_count")
    private Integer childrenCount;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(length = 30)
    @Builder.Default
    private String status = "PENDING"; 
    // PENDING (Đợi cọc), CONFIRMED (Đã cọc/gán bàn), COMPLETED (Đã check-in), CANCELLED, NO_SHOW

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "deposit_amount", precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal depositAmount = BigDecimal.ZERO;

    // Lưu giỏ hàng chọn trước dưới dạng JSON
    @JdbcTypeCode(JSON)
    @Column(name = "pre_order_draft", columnDefinition = "jsonb")
    private String preOrderDraft; 

    @Column(name = "payos_payment_link_id", length = 100)
    private String payosPaymentLinkId;

    @Column(name = "payos_order_code")
    private Long payosOrderCode;

    @Column(name = "refund_status", length = 30)
    @Builder.Default
    private String refundStatus = "NOT_REQUIRED"; 
    // NOT_REQUIRED, PENDING_REFUND, REFUNDED

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToMany
    @JoinTable(
            name = "reservation_tables",
            schema = "orders",
            joinColumns = @JoinColumn(name = "reservation_id"),
            inverseJoinColumns = @JoinColumn(name = "table_id")
    )
    @Builder.Default
    private List<TableInfo> tables = new ArrayList<>();
}
