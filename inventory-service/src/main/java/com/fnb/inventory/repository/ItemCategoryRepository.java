package com.fnb.inventory.repository;

import com.fnb.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, UUID> {
    boolean existsByName(String name);

    @Query("SELECT c FROM ItemCategory c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<ItemCategory> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
