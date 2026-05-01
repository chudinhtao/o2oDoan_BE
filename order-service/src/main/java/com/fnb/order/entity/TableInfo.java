package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "tables", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private Integer number;

    @Column(length = 50)
    private String name;

    @Column(name = "qr_token", unique = true, length = 100)
    private String qrToken;

    @Column(name = "qr_url", length = 500)
    private String qrUrl;

    @Column(length = 20)
    @Builder.Default
    private String status = "FREE"; // FREE, OCCUPIED, PAYMENT_REQUESTED, CLEANING

    @Builder.Default
    private Integer capacity = 4;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true;

    @Column(length = 50)
    private String zone; // Kẻu vực: Tầng 1, Ban công, VIP...

    @Column(name = "parent_table_id")
    private UUID parentTableId;
}
