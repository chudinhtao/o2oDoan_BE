package com.fnb.kds.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Read-only mirror of table_sessions used by kds-service
 * to retrieve the sessionToken for WebSocket routing.
 */
@Entity
@Table(name = "table_sessions", schema = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KdsTableSession {

    @Id
    private UUID id;

    @Column(name = "session_token", nullable = false)
    private String sessionToken;
}
