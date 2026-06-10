package com.fnb.inventory.repository;

import com.fnb.inventory.entity.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {
    java.util.Optional<InventoryBatch> findFirstByItemIdAndLotNumber(UUID itemId, String lotNumber);
}
