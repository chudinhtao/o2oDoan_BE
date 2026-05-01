package com.fnb.menu.repository;

import com.fnb.menu.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {

    Optional<Promotion> findByCodeAndActiveTrue(String code);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.active = true
          AND (p.startAt IS NULL OR p.startAt <= :now)
          AND (p.endAt IS NULL OR p.endAt >= :now)
    """)
    List<Promotion> findAllActive(@Param("now") LocalDateTime now);

    @Query("""
        SELECT p FROM Promotion p
        WHERE p.active = true
          AND p.scope = :scope
          AND (p.startAt IS NULL OR p.startAt <= :now)
          AND (p.endAt IS NULL OR p.endAt >= :now)
        ORDER BY p.priority DESC
    """)
    List<Promotion> findActiveByScope(@Param("scope") String scope, @Param("now") LocalDateTime now);

    @Query("""
        SELECT p FROM Promotion p WHERE
        (LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(p.code) LIKE LOWER(CONCAT('%', :keyword, '%')))
    """)
    Page<Promotion> findAllForAdmin(@Param("keyword") String keyword, Pageable pageable);
    @Query("""
        SELECT p FROM Promotion p
        WHERE p.active = true
          AND p.endAt IS NOT NULL
          AND p.endAt < :now
    """)
    List<Promotion> findAllExpiredActive(@Param("now") LocalDateTime now);

    @Modifying
    @Query("""
        UPDATE Promotion p
        SET p.active = false
        WHERE p.active = true
          AND p.endAt IS NOT NULL
          AND p.endAt < :now
    """)
    int bulkExpire(@Param("now") LocalDateTime now);
}
