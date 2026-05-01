package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_item_options", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_item_id")
    private OrderTicketItem ticketItem;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "extra_price")
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;
}
