package com.fnb.order.repository;

import com.fnb.order.entity.OrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OrderTicketRepository extends JpaRepository<OrderTicket, UUID> {
    List<OrderTicket> findByOrderId(UUID orderId);

    @Query(value = "SELECT t.item_id FROM orders.order_tickets t " +
            "JOIN orders.orders o ON t.order_id = o.id " +
            "WHERE o.created_at >= :startOfDay " +
            "GROUP BY t.item_id " +
            "ORDER BY SUM(t.quantity) DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<UUID> findTopSellingItemIds(@Param("startOfDay") java.time.LocalDateTime startOfDay, @Param("limit") int limit);

    /**
     * Chuyển toàn bộ ticket từ sourceOrder sang targetOrder ở cấp SQL,
     * bypass Hibernate orphanRemoval để tránh mất data khi gộp bàn.
     * Đồng thời cộng dồn seq_number để tránh trùng.
     */
    @Modifying
    @Query(value = "UPDATE orders.order_tickets SET order_id = :targetOrderId, " +
            "seq_number = seq_number + :seqOffset " +
            "WHERE order_id = :sourceOrderId", nativeQuery = true)
    int transferTicketsToOrder(
            @Param("sourceOrderId") UUID sourceOrderId,
            @Param("targetOrderId") UUID targetOrderId,
            @Param("seqOffset") int seqOffset);
}
