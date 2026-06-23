package com.fnb.order.repository;

import com.fnb.order.entity.Order;
import com.fnb.order.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;

public interface TableSessionRepository extends JpaRepository<TableSession, UUID> {
    Optional<TableSession> findBySessionToken(String sessionToken);
    
    @Query("SELECT s FROM TableSession s WHERE s.table.id = :tableId AND s.status = 'ACTIVE' ORDER BY s.openedAt DESC LIMIT 1")
    Optional<TableSession> findActiveSessionByTableId(@Param("tableId") UUID tableId);

    @Modifying
    @Query("UPDATE TableSession s SET s.status = 'CLOSED', s.closedAt = CURRENT_TIMESTAMP WHERE s.status = 'ACTIVE' AND s.expiresAt < CURRENT_TIMESTAMP AND NOT EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.status IN ('OPEN', 'PAYMENT_REQUESTED') AND o.total > 0)")
    int closeExpiredSessions();

    @Modifying
    @Query("UPDATE TableSession s SET s.status = 'CLOSED', s.closedAt = CURRENT_TIMESTAMP WHERE s.status = 'ACTIVE'")
    int closeAllActiveSessions();

    @Query("SELECT s FROM TableSession s WHERE s.status = 'ACTIVE' AND s.table IS NOT NULL AND NOT EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.status NOT IN ('PAID', 'CANCELLED')) AND EXISTS (SELECT o2 FROM Order o2 WHERE o2.session = s) AND (SELECT MAX(o3.updatedAt) FROM Order o3 WHERE o3.session = s) < :limit")
    List<TableSession> findSessionsReadyForCleanup(@Param("limit") LocalDateTime limit);

    @Query("SELECT s FROM TableSession s WHERE s.status = 'ACTIVE' AND s.table IS NOT NULL AND s.openedAt < :limit AND NOT EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.total > 0)")
    List<TableSession> findEmptySessions(@Param("limit") LocalDateTime limit);

    @Query("SELECT s FROM TableSession s WHERE s.status = 'ACTIVE' AND s.table IS NOT NULL AND s.openedAt < :limit AND EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.status = 'OPEN')")
    List<TableSession> findLongRunningSessions(@Param("limit") LocalDateTime limit);
}
