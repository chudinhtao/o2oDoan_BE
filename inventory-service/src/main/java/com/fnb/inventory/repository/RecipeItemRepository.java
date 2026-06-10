package com.fnb.inventory.repository;

import com.fnb.inventory.entity.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RecipeItemRepository extends JpaRepository<RecipeItem, UUID> {
}
