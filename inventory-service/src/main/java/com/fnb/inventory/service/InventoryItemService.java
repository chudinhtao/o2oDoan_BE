package com.fnb.inventory.service;

import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.InventoryItemRequest;
import com.fnb.inventory.dto.response.InventoryItemResponse;
import com.fnb.inventory.entity.InventoryItem;
import com.fnb.inventory.entity.InventoryLevel;
import com.fnb.inventory.entity.ItemCategory;
import com.fnb.inventory.entity.Uom;
import com.fnb.inventory.repository.InventoryItemRepository;
import com.fnb.inventory.repository.InventoryLevelRepository;
import com.fnb.inventory.repository.ItemCategoryRepository;
import com.fnb.inventory.repository.UomRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryItemService {

    private final InventoryItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final UomRepository uomRepository;
    private final InventoryLevelRepository levelRepository;
    private final UomService uomService;
    private final ItemCategoryService categoryService;

    public PageResponse<InventoryItemResponse> findAll(UUID categoryId, ItemType type, Boolean isActive, String keyword, int page, int size) {
        Page<InventoryItem> result = itemRepository.findAllWithFilter(
                categoryId, type != null ? type.name() : null, isActive, keyword,
                PageRequest.of(page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "created_at")));
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                page, size, result.getTotalElements());
    }

    public InventoryItemResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public InventoryItemResponse create(InventoryItemRequest request) {
        String sku = request.getSku();
        if (sku == null || sku.trim().isEmpty()) {
            sku = generateSku(request.getType());
        }

        if (itemRepository.existsBySku(sku)) {
            // Nếu mã tự sinh bị trùng (hiếm gặp), sinh lại mã mới hoặc báo lỗi
            if (request.getSku() == null || request.getSku().trim().isEmpty()) {
                sku = generateSku(request.getType()); 
            } else {
                throw new BusinessException("SKU '" + sku + "' đã tồn tại");
            }
        }

        Uom baseUom = uomRepository.findById(request.getBaseUomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy UoM: " + request.getBaseUomId()));

        ItemCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm NL: " + request.getCategoryId()));
        }

        InventoryItem item = InventoryItem.builder()
                .sku(sku)
                .name(request.getName())
                .category(category)
                .type(request.getType())
                .baseUom(baseUom)
                .safetyStock(request.getSafetyStock())
                .avgCostPrice(request.getAvgCostPrice())
                .build();
        item = itemRepository.save(item);

        // Do not auto-create empty system location records.
        // Inventory level will be created upon first Goods Receipt.

        return toResponse(item);
    }

    @Transactional
    public InventoryItemResponse update(UUID id, InventoryItemRequest request) {
        InventoryItem item = getOrThrow(id);

        Uom baseUom = uomRepository.findById(request.getBaseUomId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy UoM: " + request.getBaseUomId()));

        ItemCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm NL: " + request.getCategoryId()));
        }

        item.setSku(request.getSku());
        item.setName(request.getName());
        item.setCategory(category);
        item.setType(request.getType());
        item.setBaseUom(baseUom);
        item.setSafetyStock(request.getSafetyStock());
        item.setAvgCostPrice(request.getAvgCostPrice());

        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public InventoryItemResponse toggleActive(UUID id) {
        InventoryItem item = getOrThrow(id);
        item.setActive(!item.isActive());
        return toResponse(itemRepository.save(item));
    }

    @Transactional
    public void delete(UUID id) {
        InventoryItem item = getOrThrow(id);
        item.setActive(false);
        itemRepository.save(item);
    }

    private InventoryItem getOrThrow(UUID id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu với ID: " + id));
    }

    private InventoryItemResponse toResponse(InventoryItem item) {
        java.math.BigDecimal currentStock = java.math.BigDecimal.ZERO;
        java.util.List<com.fnb.inventory.dto.response.InventoryBatchResponse> batches = new java.util.ArrayList<>();
        
        java.util.List<InventoryLevel> levels = levelRepository.findByItemId(item.getId());
        if (!levels.isEmpty()) {
            currentStock = levels.stream()
                    .map(InventoryLevel::getCurrentStock)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
                    
            batches = levels.stream()
                    .map(lvl -> com.fnb.inventory.dto.response.InventoryBatchResponse.builder()
                            .id(lvl.getBatch() != null ? lvl.getBatch().getId() : lvl.getId())
                            .lotNumber(lvl.getBatch() != null ? lvl.getBatch().getLotNumber() : "N/A")
                            .expiryDate(lvl.getBatch() != null ? lvl.getBatch().getExpiryDate() : null)
                            .currentStock(lvl.getCurrentStock())
                            .locationId(lvl.getLocation() != null ? lvl.getLocation().getId() : null)
                            .locationName(lvl.getLocation() != null ? lvl.getLocation().getName() : "Kho Hệ Thống")
                            .build())
                    .collect(java.util.stream.Collectors.toList());
        }

        return InventoryItemResponse.builder()
                .id(item.getId())
                .sku(item.getSku())
                .name(item.getName())
                .type(item.getType())
                .safetyStock(item.getSafetyStock())
                .avgCostPrice(item.getAvgCostPrice())
                .currentStock(currentStock)
                .isActive(item.isActive())
                .category(item.getCategory() != null ? categoryService.toResponse(item.getCategory()) : null)
                .baseUom(item.getBaseUom() != null ? uomService.toResponse(item.getBaseUom()) : null)
                .batches(batches)
                .build();
    }

    private String generateSku(ItemType type) {
        String prefix = "RM"; // Default Raw Material
        if (type == ItemType.RETAIL) prefix = "RT";
        if (type == ItemType.CONSUMABLE) prefix = "CS";
        
        int randomNum = new java.util.Random().nextInt(900000) + 100000;
        return prefix + randomNum;
    }
}
