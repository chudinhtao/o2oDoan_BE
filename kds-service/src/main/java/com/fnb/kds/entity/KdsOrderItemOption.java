package com.fnb.kds.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "order_item_options", schema = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsOrderItemOption {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_item_id")
    private KdsOrderTicketItem ticketItem;

    @Column(name = "option_name")
    private String optionName;

    @Column(name = "extra_price")
    private BigDecimal extraPrice;
}
