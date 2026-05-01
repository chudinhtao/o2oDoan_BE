package com.fnb.menu.repository;

import com.fnb.menu.entity.ItemOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ItemOptionRepository extends JpaRepository<ItemOption, UUID> {

    @Query("SELECT o FROM ItemOption o JOIN o.group g JOIN g.item i WHERE o.id = :optionId AND i.id = :itemId")
    Optional<ItemOption> findByIdAndItemId(UUID optionId, UUID itemId);
}
