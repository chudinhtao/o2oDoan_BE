package com.fnb.inventory.repository;

import com.fnb.inventory.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SupplierRepository extends JpaRepository<Supplier, UUID> {
    boolean existsByCode(String code);

    @Query(value = """
        SELECT s.* FROM inventory.suppliers s WHERE
        (CAST(:isActive AS boolean) IS NULL OR s.is_active = CAST(:isActive AS boolean))
        AND (CAST(:keyword AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
    """, countQuery = """
        SELECT count(s.id) FROM inventory.suppliers s WHERE
        (CAST(:isActive AS boolean) IS NULL OR s.is_active = CAST(:isActive AS boolean))
        AND (CAST(:keyword AS text) IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.code) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.phone) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%'))
         OR LOWER(s.email) LIKE LOWER(CONCAT('%', CAST(:keyword AS text), '%')))
    """, nativeQuery = true)
    Page<Supplier> findAllWithFilter(@Param("keyword") String keyword, @Param("isActive") Boolean isActive, Pageable pageable);
}
