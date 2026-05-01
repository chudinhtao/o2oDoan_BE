package com.fnb.menu.repository;

import com.fnb.menu.entity.MenuItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    @Modifying
    @Query("UPDATE MenuItem m SET m.isFeatured = false")
    int resetAllFeatured();

    @Modifying
    @Query("UPDATE MenuItem m SET m.isFeatured = true WHERE m.id IN :itemIds")
    int setFeaturedItems(@Param("itemIds") List<UUID> itemIds);

    // JOIN FETCH chỉ optionGroups — options load tự động qua @BatchSize(30) trên ItemOptionGroup
    @Query("SELECT DISTINCT m FROM MenuItem m LEFT JOIN FETCH m.optionGroups " +
           "WHERE m.id = :id")
    Optional<MenuItem> findByIdWithOptions(UUID id);

    Page<MenuItem> findByIsAvailableTrueAndIsActiveTrueAndIsFeaturedTrue(Pageable pageable);

    @Query(value = "SELECT m FROM MenuItem m WHERE m.category.id = :categoryId AND m.isActive = true AND m.isAvailable = true",
           countQuery = "SELECT COUNT(m) FROM MenuItem m WHERE m.category.id = :categoryId AND m.isActive = true AND m.isAvailable = true")
    Page<MenuItem> findByCategoryPublic(@Param("categoryId") UUID categoryId, Pageable pageable);

    @Query(value = "SELECT m FROM MenuItem m WHERE m.isActive = true AND m.isAvailable = true",
           countQuery = "SELECT COUNT(m) FROM MenuItem m WHERE m.isActive = true AND m.isAvailable = true")
    Page<MenuItem> findAllActivePublic(Pageable pageable);

    // ─── Customer: tìm kiếm công khai (kèm keyword và maxPrice) ─────────
    @Query(value = """
        SELECT m FROM MenuItem m
        WHERE m.isActive = true AND m.isAvailable = true
        AND (:categoryId IS NULL OR m.category.id = :categoryId)
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:maxPrice IS NULL OR m.salePrice <= :maxPrice OR (m.salePrice IS NULL AND m.basePrice <= :maxPrice))
    """, countQuery = """
        SELECT COUNT(m) FROM MenuItem m
        WHERE m.isActive = true AND m.isAvailable = true
        AND (:categoryId IS NULL OR m.category.id = :categoryId)
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
        AND (:maxPrice IS NULL OR m.salePrice <= :maxPrice OR (m.salePrice IS NULL AND m.basePrice <= :maxPrice))
    """)
    Page<MenuItem> searchForCustomer(@Param("categoryId") UUID categoryId,
                                     @Param("keyword") String keyword,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     Pageable pageable);

    // ─── Admin: list tất cả món (cả ẩn), có filter + keyword + pagination ─────────
    @Query(value = """
        SELECT m FROM MenuItem m
        WHERE (:categoryId IS NULL OR m.category.id = :categoryId)
        AND (:isActive IS NULL OR m.isActive = :isActive)
        AND (:isAvailable IS NULL OR m.isAvailable = :isAvailable)
        AND (:isFeatured IS NULL OR m.isFeatured = :isFeatured)
        AND (:station IS NULL OR m.station = :station)
        AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """,
            countQuery = """
        SELECT COUNT(m) FROM MenuItem m
        WHERE (:categoryId IS NULL OR m.category.id = :categoryId)
        AND (:isActive IS NULL OR m.isActive = :isActive)
        AND (:isAvailable IS NULL OR m.isAvailable = :isAvailable)
        AND (:isFeatured IS NULL OR m.isFeatured = :isFeatured)
        AND (:station IS NULL OR m.station = :station)
        AND LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<MenuItem> findAllForAdmin(@Param("categoryId") UUID categoryId,
                                   @Param("isActive") Boolean isActive,
                                   @Param("isAvailable") Boolean isAvailable,
                                   @Param("isFeatured") Boolean isFeatured,
                                   @Param("station") String station,
                                   @Param("keyword") String keyword,
                                   Pageable pageable);

    List<MenuItem> findByCategoryId(UUID categoryId);
}
