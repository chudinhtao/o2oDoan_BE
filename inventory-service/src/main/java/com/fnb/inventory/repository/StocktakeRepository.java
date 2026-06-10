package com.fnb.inventory.repository;

import com.fnb.inventory.entity.Stocktake;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface StocktakeRepository extends JpaRepository<Stocktake, UUID> {
    @Query(value = "SELECT s.* FROM inventory.stocktakes s " +
                   "WHERE (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text)) " +
                   "AND (CAST(:keyword AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR CAST(s.id AS text) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
                   "AND (CAST(:startDate AS text) IS NULL OR s.created_at >= CAST(:startDate AS timestamp)) " +
                   "AND (CAST(:endDate AS text) IS NULL OR s.created_at <= CAST(:endDate AS timestamp)) ORDER BY s.created_at DESC", 
           countQuery = "SELECT count(s.id) FROM inventory.stocktakes s " +
                        "WHERE (CAST(:status AS text) IS NULL OR s.status = CAST(:status AS text)) " +
                        "AND (CAST(:keyword AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')) OR CAST(s.id AS text) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))) " +
                        "AND (CAST(:startDate AS text) IS NULL OR s.created_at >= CAST(:startDate AS timestamp)) " +
                        "AND (CAST(:endDate AS text) IS NULL OR s.created_at <= CAST(:endDate AS timestamp))",
           nativeQuery = true)
    Page<Stocktake> findWithFilter(
            @Param("status") String status, 
            @Param("keyword") String keyword,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate,
            Pageable pageable);
}
