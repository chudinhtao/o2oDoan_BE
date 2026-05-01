package com.fnb.order.repository;

import com.fnb.order.entity.OrderTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTicketItemRepository extends JpaRepository<OrderTicketItem, UUID> {

    /**
     * Lấy tất cả món DONE hoặc COMPLETED chưa được SERVED.
     * Dùng cho màn hình Dashboard của Server.
     * JOIN qua ticket -> order để lấy thông tin bàn.
     */
    @Query(value = """
            SELECT oti.* FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders.orders o ON ot.order_id = o.id
            WHERE oti.status IN ('DONE', 'COMPLETED')
              AND oti.served_at IS NULL
            ORDER BY oti.created_at ASC
            """, nativeQuery = true)
    List<OrderTicketItem> findPendingDeliveries();

    /**
     * Có filter theo Zone: chỉ lấy các món thuộc bàn trong danh sách zone.
     * Nếu zones rỗng/null → trả về toàn bộ (tương đương findPendingDeliveries).
     */
    @Query(value = """
            SELECT oti.* FROM orders.order_ticket_items oti
            JOIN orders.order_tickets ot ON oti.ticket_id = ot.id
            JOIN orders.orders o ON ot.order_id = o.id
            JOIN orders.tables t ON o.table_id = t.id
            WHERE oti.status IN ('DONE', 'COMPLETED')
              AND oti.served_at IS NULL
              AND (:zones IS NULL OR t.zone = ANY(CAST(:zones AS text[])))
            ORDER BY oti.created_at ASC
            """, nativeQuery = true)
    List<OrderTicketItem> findPendingDeliveriesByZones(@Param("zones") String zones);

    /**
     * Đếm số món còn lại đang PENDING/PREPARING trong cùng ticket.
     * Dùng cho Dynamic Batching Nguyên tắc 2: nếu = 0 thì đây là món cuối → Delay=0s.
     */
    @Query("SELECT COUNT(i) FROM OrderTicketItem i WHERE i.ticket.id = :ticketId " +
            "AND i.status IN ('PENDING', 'PREPARING') AND i.id != :excludeItemId")
    long countRemainingActiveItemsInTicket(
            @Param("ticketId") UUID ticketId,
            @Param("excludeItemId") UUID excludeItemId
    );

    /**
     * Lấy các món DONE/COMPLETED chưa SERVED sau một thời điểm nhất định.
     * Dùng cho Sweeper Job để tìm món bị trễ.
     */
    @Query(value = """
            SELECT oti.* FROM orders.order_ticket_items oti
            WHERE oti.status IN ('DONE', 'COMPLETED')
              AND oti.served_at IS NULL
              AND oti.created_at <= :threshold
            """, nativeQuery = true)
    List<OrderTicketItem> findOverdueDeliveries(@Param("threshold") LocalDateTime threshold);

    /**
     * Bulk update: Đánh dấu SERVED cho nhiều món một lúc.
     * Chỉ update các món đang ở status DONE/COMPLETED (tránh update nhầm).
     * Returns số bản ghi bị ảnh hưởng.
     */
    @Modifying
    @Query(value = """
            UPDATE orders.order_ticket_items
            SET status = 'SERVED', served_at = :servedAt, served_by = :servedBy
            WHERE id IN :itemIds
              AND status IN ('DONE', 'COMPLETED')
              AND served_at IS NULL
            """, nativeQuery = true)
    int markAsServed(
            @Param("itemIds") List<UUID> itemIds,
            @Param("servedAt") LocalDateTime servedAt,
            @Param("servedBy") UUID servedBy
    );

    /**
     * Undo serve: Rollback về DONE nếu served_at trong vòng 30 giây.
     * Điều kiện served_at >= :cutoff đảm bảo chỉ Undo được trong cửa sổ thời gian.
     */
    @Modifying
    @Query(value = """
            UPDATE orders.order_ticket_items
            SET status = 'DONE', served_at = NULL, served_by = NULL
            WHERE id IN :itemIds
              AND status = 'SERVED'
              AND served_at >= :cutoff
            """, nativeQuery = true)
    int undoServe(
            @Param("itemIds") List<UUID> itemIds,
            @Param("cutoff") LocalDateTime cutoff
    );

    /** KPI: T\u1ed5ng m\u00f3n \u0111\u00e3 b\u01b0ng c\u1ee7a nh\u00e2n vi\u00ean h\u00f4m nay. */
    @Query(value = "SELECT COUNT(*) FROM orders.order_ticket_items " +
            "WHERE served_by = :serverId AND served_at >= :startOfDay",
            nativeQuery = true)
    long countServedToday(
            @Param("serverId") UUID serverId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    List<OrderTicketItem> findByIdIn(List<UUID> ids);
}
