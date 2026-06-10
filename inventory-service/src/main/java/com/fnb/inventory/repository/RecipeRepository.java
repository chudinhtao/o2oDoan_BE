package com.fnb.inventory.repository;

import com.fnb.inventory.entity.Recipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.fnb.inventory.enums.RecipeType;

public interface RecipeRepository extends JpaRepository<Recipe, UUID> {

    @Query("""
        SELECT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem LEFT JOIN FETCH ri.uom
        WHERE r.saleItemId = :saleItemId AND r.type = 'MAIN_ITEM'
    """)
    Optional<Recipe> findBySaleItemIdWithItems(@Param("saleItemId") UUID saleItemId);

    @Query("""
        SELECT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem LEFT JOIN FETCH ri.uom
        WHERE r.modifierId = :modifierId AND r.type = 'MODIFIER'
    """)
    Optional<Recipe> findByModifierIdWithItems(@Param("modifierId") UUID modifierId);

    Optional<Recipe> findBySaleItemIdAndType(UUID saleItemId, RecipeType type);
    Optional<Recipe> findByModifierIdAndType(UUID modifierId, RecipeType type);

    @Query("""
        SELECT r FROM Recipe r LEFT JOIN FETCH r.items ri LEFT JOIN FETCH ri.inventoryItem LEFT JOIN FETCH ri.uom
        WHERE r.id = :id
    """)
    Optional<Recipe> findByIdWithItems(@Param("id") UUID id);

    List<Recipe> findByType(RecipeType type);

    /** Tìm tất cả Recipe có chứa inventoryItemId trong danh sách nguyên liệu */
    @Query("""
        SELECT DISTINCT r FROM Recipe r
        JOIN r.items ri
        WHERE ri.inventoryItem.id = :inventoryItemId
    """)
    List<Recipe> findByInventoryItemId(@Param("inventoryItemId") UUID inventoryItemId);
}
