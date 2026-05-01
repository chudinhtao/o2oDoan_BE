package com.fnb.kds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_ticket_items", schema = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsOrderTicketItem {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id")
    private KdsOrderTicket ticket;

    @Column(name = "item_name")
    private String itemName;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "status")
    private String status;
    
    @Column(name = "unit_price")
    private java.math.BigDecimal unitPrice;
    
    @Column(name = "station")
    private String station;

    @Column(name = "note")
    private String note;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** Phase 1 — Kitchen KPI: Ai là đầu bếp đã làm xong món này? */
    @Column(name = "prepared_by")
    private UUID preparedBy;

    /** Phase 1 — Kitchen KPI: Món này được làm xong lúc mấy giờ? (Tính tốc độ bếp) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    @OneToMany(mappedBy = "ticketItem", fetch = FetchType.LAZY)
    @Builder.Default
    private List<KdsOrderItemOption> options = new ArrayList<>();
}
