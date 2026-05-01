package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private TableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableInfo table;

    @Column(nullable = false, length = 20)
    private String source; // QR (Từ khách quét mã) hoặc MANUAL (Nhân viên tạo tại POS)

    @Column(name = "order_type", length = 20)
    @Builder.Default
    private String orderType = "DINE_IN"; // DINE_IN, TAKEAWAY, DELIVERY

    @Column(length = 30)
    @Builder.Default
    private String status = "OPEN"; 
    // OPEN: Đang gọi món
    // PAYMENT_REQUESTED: Đã gửi yêu cầu thanh toán
    // PAID: Đã thanh toán
    // CANCELLED: Hủy nguyên bill

    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal discount = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal tax = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal serviceFee = BigDecimal.ZERO;

    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "promotion_id")
    private UUID promotionId;

    @Column(name = "promotion_code", length = 50)
    private String promotionCode;

    @Column(name = "payos_order_code")
    private Long payosOrderCode;

    @Column(name = "discount_type", length = 20)
    private String discountType; // PERCENT, AMOUNT

    @Column(name = "discount_rate", precision = 12, scale = 2)
    private BigDecimal discountRate; // Giá trị % hoặc số tiền tùy type

    @Column(name = "min_order_amount", precision = 12, scale = 2)
    private BigDecimal minOrderAmount; // Điều kiện tối thiểu để duy trì mã

    @Column(name = "max_discount_value", precision = 12, scale = 2)
    private BigDecimal maxDiscountValue; // Giới hạn mức giảm tối đa (cho loại %)

    @Column(name = "is_stackable")
    @Builder.Default
    private Boolean isStackable = true;

    @Column(name = "payment_method", length = 30)
    private String paymentMethod; // CASH, PayOS, MIXED

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "payment_detail", columnDefinition = "jsonb")
    private String paymentDetail;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    /** Phase 1 — Staff Tracking: Ai là thu ngân đã chốt thanh toán đơn này? */
    @Column(name = "cashier_id")
    private UUID cashierId;

    /** Phase 1 — Fraud Prevention: Ai là người đã duyệt hủy và lý do hủy là gì? */
    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderTicket> tickets = new ArrayList<>();
}
