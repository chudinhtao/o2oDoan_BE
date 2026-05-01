package com.fnb.order.repository;

import com.fnb.order.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TableSessionRepository extends JpaRepository<TableSession, UUID> {
    Optional<TableSession> findBySessionToken(String sessionToken);
    
    @Query("SELECT s FROM TableSession s WHERE s.table.id = :tableId AND s.status = 'ACTIVE' AND s.expiresAt > CURRENT_TIMESTAMP ORDER BY s.openedAt DESC LIMIT 1")
    Optional<TableSession> findActiveSessionByTableId(@Param("tableId") UUID tableId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableSession s SET s.status = 'CLOSED', s.closedAt = CURRENT_TIMESTAMP WHERE s.status = 'ACTIVE' AND s.expiresAt < CURRENT_TIMESTAMP AND NOT EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.status IN ('OPEN', 'PAYMENT_REQUESTED') AND o.total > 0)")
    int closeExpiredSessions();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableSession s SET s.status = 'CLOSED', s.closedAt = CURRENT_TIMESTAMP WHERE s.status = 'ACTIVE'")
    int closeAllActiveSessions();
}
