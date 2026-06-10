package com.fnb.inventory.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.RecipeRequest;
import com.fnb.inventory.dto.response.RecipeResponse;
import com.fnb.inventory.dto.response.UomResponse;
import com.fnb.inventory.entity.InventoryItem;
import com.fnb.inventory.entity.Recipe;
import com.fnb.inventory.entity.RecipeItem;
import com.fnb.inventory.entity.Uom;
import com.fnb.inventory.repository.InventoryItemRepository;
import com.fnb.inventory.repository.RecipeRepository;
import com.fnb.inventory.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final InventoryItemRepository itemRepository;
    private final com.fnb.inventory.repository.UomRepository uomRepository;
    private final LocationRepository locationRepository;
    private final UomConversionService uomConversionService;

    public RecipeResponse findById(UUID id) {
        Recipe recipe = recipeRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found: " + id));
        return toResponse(recipe);
    }

    public RecipeResponse findBySaleItemId(UUID saleItemId) {
        Recipe recipe = recipeRepository.findBySaleItemIdWithItems(saleItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found for item: " + saleItemId));
        return toResponse(recipe);
    }

    public RecipeResponse findByModifierId(UUID modifierId) {
        Recipe recipe = recipeRepository.findByModifierIdWithItems(modifierId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipe not found for modifier: " + modifierId));
        return toResponse(recipe);
    }

    public List<RecipeResponse> findAllByType(RecipeType type) {
        return recipeRepository.findByType(type).stream().map(r -> {
            Recipe full = recipeRepository.findByIdWithItems(r.getId()).orElse(r);
            return toResponse(full);
        }).toList();
    }

    @Transactional
    public RecipeResponse createOrUpdate(RecipeRequest request) {
        validateRequest(request);

        Recipe recipe;
        if (RecipeType.MAIN_ITEM.equals(request.getType())) {
            if (request.getSaleItemId() == null) throw new BusinessException("saleItemId is required for MAIN_ITEM");
            recipe = recipeRepository.findBySaleItemIdAndType(request.getSaleItemId(), RecipeType.MAIN_ITEM)
                    .orElse(Recipe.builder().saleItemId(request.getSaleItemId()).type(RecipeType.MAIN_ITEM).build());
        } else if (RecipeType.MODIFIER.equals(request.getType())) {
            if (request.getModifierId() == null) throw new BusinessException("modifierId is required for MODIFIER");
            recipe = recipeRepository.findByModifierIdAndType(request.getModifierId(), RecipeType.MODIFIER)
                    .orElse(Recipe.builder().modifierId(request.getModifierId()).type(RecipeType.MODIFIER).build());
        } else {
            throw new BusinessException("type must be MAIN_ITEM or MODIFIER");
        }

        if (request.getDefaultLocationId() != null) {
            com.fnb.inventory.entity.Location loc = locationRepository.findById(request.getDefaultLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Location not found"));
            recipe.setDefaultLocation(loc);
        } else {
            recipe.setDefaultLocation(null);
        }

        // Clear old items and rebuild
        recipe.getItems().clear();

        for (RecipeRequest.RecipeItemRequest ri : request.getItems()) {
            InventoryItem invItem = itemRepository.findById(ri.getInventoryItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Inventory item not found: " + ri.getInventoryItemId()));
            Uom uom = uomRepository.findById(ri.getUomId())
                    .orElseThrow(() -> new ResourceNotFoundException("UoM not found: " + ri.getUomId()));

            // Validate UoM compatibility immediately to prevent crashes later
            uomConversionService.calculateBaseQuantity(invItem, uom.getId(), BigDecimal.ONE);

            RecipeItem recipeItem = RecipeItem.builder()
                    .recipe(recipe)
                    .inventoryItem(invItem)
                    .quantity(ri.getQuantity())
                    .uom(uom)
                    .wastagePercent(ri.getWastagePercent() != null ? ri.getWastagePercent() : BigDecimal.ZERO)
                    .scope(ri.getScope() != null ? ri.getScope() : IngredientScope.ALWAYS)
                    .build();
            recipe.getItems().add(recipeItem);
        }

        recipe = recipeRepository.save(recipe);
        return toResponse(recipeRepository.findByIdWithItems(recipe.getId()).orElse(recipe));
    }

    @Transactional
    public void delete(UUID id) {
        if (!recipeRepository.existsById(id)) throw new ResourceNotFoundException("Recipe not found: " + id);
        recipeRepository.deleteById(id);
    }

    private void validateRequest(RecipeRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException("Recipe must have at least 1 ingredient");
        }
    }

    private RecipeResponse toResponse(Recipe r) {
        List<RecipeResponse.RecipeItemResponse> items = r.getItems().stream().map(ri ->
                RecipeResponse.RecipeItemResponse.builder()
                        .id(ri.getId())
                        .inventoryItemId(ri.getInventoryItem().getId())
                        .inventoryItemName(ri.getInventoryItem().getName())
                        .quantity(ri.getQuantity())
                        .uom(UomResponse.builder()
                                .id(ri.getUom().getId())
                                .name(ri.getUom().getName())
                                .shortName(ri.getUom().getShortName())
                                .build())
                        .wastagePercent(ri.getWastagePercent())
                        .scope(ri.getScope())
                        .build()
        ).toList();

        return RecipeResponse.builder()
                .id(r.getId())
                .saleItemId(r.getSaleItemId())
                .modifierId(r.getModifierId())
                .type(r.getType())
                .defaultLocationId(r.getDefaultLocation() != null ? r.getDefaultLocation().getId() : null)
                .defaultLocationName(r.getDefaultLocation() != null ? r.getDefaultLocation().getName() : null)
                .items(items)
                .build();
    }
}
