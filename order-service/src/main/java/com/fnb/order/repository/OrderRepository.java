package com.fnb.order.repository;

import com.fnb.order.dto.response.PosTableResponse;
import com.fnb.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.time.LocalDateTime;
import java.util.Collection;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Optional<Order> findBySessionIdAndStatus(UUID sessionId, String status);
    Optional<Order> findFirstBySessionIdAndStatusIn(UUID sessionId, Collection<String> statuses);
    boolean existsBySessionIdAndStatus(UUID sessionId, String status);
    Optional<Order> findFirstBySessionIdOrderByUpdatedAtDesc(UUID sessionId);
    Optional<Order> findByPayosOrderCode(Long payosOrderCode);
    List<Order> findByStatus(String status);
    List<Order> findByStatusAndPaymentMethod(String status, String paymentMethod);
    List<Order> findTop50ByStatusAndPaymentMethodAndUpdatedAtGreaterThanEqualOrderByUpdatedAtAsc(String status, String paymentMethod, LocalDateTime time);
    Optional<Order> findBySession_SessionToken(String sessionToken);

    /**
     * Lấy danh sách đơn Takeaway đang OPEN/PAYMENT_REQUESTED
     * dưới dạng PosTableResponse để FE render lưới giống bàn.
     */
    @Query("""
        SELECT new com.fnb.order.dto.response.PosTableResponse(
            o.id,
            null,
            null,
            CASE WHEN o.status = 'OPEN' THEN 'OCCUPIED' ELSE o.status END,
            0,
            s.id, s.sessionToken,
            COALESCE(o.total, 0.0),
            s.openedAt,
            null, null, null
        )
        FROM Order o
        JOIN o.session s
        WHERE o.orderType = 'TAKEAWAY'
          AND o.status IN ('OPEN', 'PAYMENT_REQUESTED')
        ORDER BY s.openedAt DESC
    """)
    List<PosTableResponse> findActiveTakeawayOrders();

    @Modifying
    @Query("DELETE FROM Order o WHERE o.tickets IS EMPTY AND o.createdAt < :limit AND o.discount = 0")
    int deleteEmptyOrders(@Param("limit") LocalDateTime limit);

    @Query("SELECT o FROM Order o WHERE o.orderType = 'TAKEAWAY' AND o.status = 'PAYMENT_REQUESTED' AND o.updatedAt < :limit")
    List<Order> findOverdueTakeawayPayments(@Param("limit") LocalDateTime limit);

    @Query("SELECT o FROM Order o WHERE o.orderType = 'DINE_IN' AND o.status = 'PAYMENT_REQUESTED' AND o.updatedAt < :limit")
    List<Order> findOverdueDineInPayments(@Param("limit") LocalDateTime limit);
}
