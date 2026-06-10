package com.fnb.inventory.repository;

import com.fnb.inventory.entity.ItemUomConversion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ItemUomConversionRepository extends JpaRepository<ItemUomConversion, UUID> {
    List<ItemUomConversion> findByItemId(UUID itemId);
}
