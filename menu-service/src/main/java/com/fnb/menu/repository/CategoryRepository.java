package com.fnb.menu.repository;

import com.fnb.menu.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Page<Category> findByIsActiveTrueOrderByDisplayOrderAsc(Pageable pageable);
    boolean existsByName(String name);

    @Query("""
        SELECT c FROM Category c WHERE 
        LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Category> findAllForAdmin(@Param("keyword") String keyword, Pageable pageable);
}
