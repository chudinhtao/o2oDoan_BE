package com.fnb.inventory.service;

import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.inventory.dto.request.ItemCategoryRequest;
import com.fnb.inventory.dto.response.ItemCategoryResponse;
import com.fnb.inventory.entity.ItemCategory;
import com.fnb.inventory.repository.ItemCategoryRepository;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.fnb.common.dto.PageResponse;

@Service
@RequiredArgsConstructor
public class ItemCategoryService {

    private final ItemCategoryRepository categoryRepository;

    public List<ItemCategoryResponse> findAll() {
        return categoryRepository.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")).stream().map(this::toResponse).toList();
    }

    public PageResponse<ItemCategoryResponse> search(String keyword, Pageable pageable) {
        Page<ItemCategory> page;
        if (keyword == null || keyword.trim().isEmpty()) {
            page = categoryRepository.findAll(pageable);
        } else {
            page = categoryRepository.searchByKeyword(keyword, pageable);
        }
        return PageResponse.of(
            page.getContent().stream().map(this::toResponse).toList(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }

    public ItemCategoryResponse findById(UUID id) {
        return toResponse(getOrThrow(id));
    }

    @Transactional
    public ItemCategoryResponse create(ItemCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException("Nhóm nguyên liệu '" + request.getName() + "' đã tồn tại");
        }
        ItemCategory category = ItemCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public ItemCategoryResponse update(UUID id, ItemCategoryRequest request) {
        ItemCategory category = getOrThrow(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        ItemCategory category = getOrThrow(id);
        categoryRepository.delete(category);
    }

    private ItemCategory getOrThrow(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhóm NL với ID: " + id));
    }

    public ItemCategoryResponse toResponse(ItemCategory c) {
        return ItemCategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .description(c.getDescription())
                .build();
    }
}
