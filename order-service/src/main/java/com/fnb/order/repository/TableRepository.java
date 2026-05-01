package com.fnb.order.repository;

import com.fnb.order.entity.TableInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

public interface TableRepository extends JpaRepository<TableInfo, UUID> {
    Optional<TableInfo> findByQrToken(String qrToken);
    Optional<TableInfo> findByNumber(Integer number);

    /** Lấy danh sách Zone duy nhất để populate dropdown cho Server App. */
    @Query("SELECT DISTINCT t.zone FROM TableInfo t WHERE t.zone IS NOT NULL AND t.isActive = true ORDER BY t.zone ASC")
    List<String> findDistinctZones();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TableInfo t WHERE t.id = :id")
    Optional<TableInfo> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TableInfo t WHERE t.qrToken = :qrToken")
    Optional<TableInfo> findByQrTokenForUpdate(@Param("qrToken") String qrToken);

    @Query("""
        SELECT t FROM TableInfo t WHERE 
        (LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR CAST(t.number AS string) LIKE CONCAT('%', :keyword, '%'))
        AND (:status IS NULL OR t.status = :status)
        AND (:isActive IS NULL OR t.isActive = :isActive)
    """)
    Page<TableInfo> findAllWithFilter(
            @Param("keyword") String keyword, 
            @Param("status") String status, 
            @Param("isActive") Boolean isActive, 
            Pageable pageable);

    @Query("""
        SELECT new com.fnb.order.dto.response.PosTableResponse(
            t.id, t.number, t.name, t.status, t.capacity,
            s.id, s.sessionToken,
            COALESCE(SUM(o.total), 0),
            s.openedAt,
            t.parentTableId,
            pt.number
        )
        FROM TableInfo t
        LEFT JOIN TableInfo pt ON t.parentTableId = pt.id
        LEFT JOIN TableSession s ON s.table = t AND s.status = 'ACTIVE'
        LEFT JOIN Order o ON o.session = s AND o.status IN ('OPEN', 'PAYMENT_REQUESTED')
        WHERE t.isActive = true
        GROUP BY t.id, t.number, t.name, t.status, t.capacity, s.id, s.sessionToken, s.openedAt, t.parentTableId, pt.number
        ORDER BY t.number ASC
    """)
    java.util.List<com.fnb.order.dto.response.PosTableResponse> findAllForPos();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableInfo t SET t.status = 'FREE', t.parentTableId = null WHERE t.status != 'FREE'")
    int resetAllTablesToFree();

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableInfo t SET t.status = 'FREE', t.parentTableId = null WHERE t.parentTableId = :parentId")
    int freeChildTables(@Param("parentId") UUID parentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableInfo t SET t.status = 'CLEANING', t.parentTableId = null WHERE t.parentTableId = :parentId")
    int cleanChildTables(@Param("parentId") UUID parentId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE TableInfo t SET t.status = 'FREE' WHERE t.id IN (SELECT s.table.id FROM TableSession s WHERE s.status = 'ACTIVE' AND s.expiresAt < CURRENT_TIMESTAMP AND NOT EXISTS (SELECT o FROM Order o WHERE o.session = s AND o.status IN ('OPEN', 'PAYMENT_REQUESTED') AND o.total > 0))")
    int resetTablesForExpiredSessions();
}
