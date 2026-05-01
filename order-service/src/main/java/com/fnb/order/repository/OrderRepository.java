package com.fnb.order.repository;

import com.fnb.order.dto.response.PosTableResponse;
import com.fnb.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {
    Optional<Order> findBySessionIdAndStatus(UUID sessionId, String status);
    Optional<Order> findFirstBySessionIdAndStatusIn(UUID sessionId, java.util.Collection<String> statuses);
    boolean existsBySessionIdAndStatus(UUID sessionId, String status);
    Optional<Order> findFirstBySessionIdOrderByUpdatedAtDesc(UUID sessionId);
    Optional<Order> findByPayosOrderCode(Long payosOrderCode);
    List<Order> findByStatus(String status);
    List<Order> findByStatusAndPaymentMethod(String status, String paymentMethod);

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
            COALESCE(o.total, 0),
            s.openedAt,
            null, null
        )
        FROM Order o
        JOIN o.session s
        WHERE o.orderType = 'TAKEAWAY'
          AND o.status IN ('OPEN', 'PAYMENT_REQUESTED')
        ORDER BY s.openedAt DESC
    """)
    List<PosTableResponse> findActiveTakeawayOrders();

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Order o WHERE o.tickets IS EMPTY AND o.createdAt < :limit")
    int deleteEmptyOrders(@org.springframework.data.repository.query.Param("limit") java.time.LocalDateTime limit);
}
