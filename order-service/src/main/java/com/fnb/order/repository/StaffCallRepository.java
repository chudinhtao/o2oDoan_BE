package com.fnb.order.repository;

import com.fnb.order.entity.StaffCall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StaffCallRepository extends JpaRepository<StaffCall, UUID> {
    List<StaffCall> findByStatusOrderByCreatedAtDesc(String status);
    List<StaffCall> findBySessionId(UUID sessionId);

    /** Lấy các yêu cầu PENDING và ACCEPTED cho màn hình Server. */
    @Query("SELECT c FROM StaffCall c WHERE c.status IN ('PENDING', 'ACCEPTED') ORDER BY c.createdAt ASC")
    List<StaffCall> findActiveServerCalls();

    /**
     * Có filter theo Zone: chỉ lấy các yêu cầu từ bàn thuộc zone chỉ định.
     * Nếu zones rỗng → trả về toàn bộ (same as findActiveServerCalls).
     */
    @Query(value = """
            SELECT sc.* FROM orders.staff_calls sc
            JOIN orders.tables t ON sc.table_id = t.id
            WHERE sc.status IN ('PENDING', 'ACCEPTED')
              AND (:zones IS NULL OR t.zone = ANY(CAST(:zones AS text[])))
            ORDER BY sc.created_at ASC
            """, nativeQuery = true)
    List<StaffCall> findActiveServerCallsByZones(@Param("zones") String zones);

    /** KPI: Tổng yêu cầu đã xử lý của nhân viên hôm nay. */
    @Query("SELECT COUNT(c) FROM StaffCall c WHERE c.resolvedBy = :serverId " +
            "AND c.resolvedAt >= :startOfDay")
    long countResolvedToday(
            @Param("serverId") UUID serverId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    /** KPI: Thời gian xử lý trung bình (giây) hôm nay. */
    @Query(value = "SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (sc.resolved_at - sc.accepted_at))), 0) " +
            "FROM orders.staff_calls sc " +
            "WHERE sc.resolved_by = :serverId " +
            "AND sc.resolved_at >= :startOfDay " +
            "AND sc.accepted_at IS NOT NULL",
            nativeQuery = true)
    double avgResolutionTimeSeconds(
            @Param("serverId") UUID serverId,
            @Param("startOfDay") LocalDateTime startOfDay
    );

    /**
     * Optimistic Lock: Tiếp nhận cuộc gọi chỉ khi status vẫn PENDING.
     * Nếu returns 0 -> đã có người accept trước -> throw 409 Conflict.
     */
    @Modifying
    @Query(value = "UPDATE orders.staff_calls " +
            "SET status = 'ACCEPTED', accepted_by = :acceptedBy, accepted_at = :acceptedAt " +
            "WHERE id = :callId AND status = 'PENDING'",
            nativeQuery = true)
    int acceptCall(
            @Param("callId") UUID callId,
            @Param("acceptedBy") UUID acceptedBy,
            @Param("acceptedAt") LocalDateTime acceptedAt
    );

    /** Lấy các StaffCall PENDING quá :threshold (cho Sweeper spillover). */
    @Query("SELECT c FROM StaffCall c WHERE c.status = 'PENDING' AND c.createdAt <= :threshold")
    List<StaffCall> findPendingCallsOlderThan(@Param("threshold") LocalDateTime threshold);

    @Modifying
    @Query("DELETE FROM StaffCall c WHERE c.status = 'RESOLVED' AND c.createdAt < :limit")
    int deleteOldResolvedCalls(@Param("limit") LocalDateTime limit);
}
