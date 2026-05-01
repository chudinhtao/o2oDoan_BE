package com.fnb.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "table_sessions", schema = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private TableInfo table;

    @Column(name = "session_token", unique = true, nullable = false, length = 100)
    private String sessionToken;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE"; // ACTIVE, CLOSED, MERGED

    @Column(name = "opened_at", updatable = false)
    @Builder.Default
    private LocalDateTime openedAt = LocalDateTime.now();

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
}
