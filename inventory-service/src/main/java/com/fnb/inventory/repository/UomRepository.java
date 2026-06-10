package com.fnb.inventory.repository;

import com.fnb.inventory.entity.Uom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UomRepository extends JpaRepository<Uom, UUID> {
    boolean existsByName(String name);

    @Query("SELECT u FROM Uom u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(u.shortName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Uom> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
