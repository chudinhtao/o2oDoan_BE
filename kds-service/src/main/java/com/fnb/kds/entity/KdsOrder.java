package com.fnb.kds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "orders", schema = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsOrder {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private KdsTableSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private KdsTable table;

    @Column(name = "subtotal")
    private java.math.BigDecimal subtotal;

    @Column(name = "total")
    private java.math.BigDecimal total;
}
