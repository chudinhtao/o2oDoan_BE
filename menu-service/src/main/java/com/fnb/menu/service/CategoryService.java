package com.fnb.menu.service;

import com.fnb.menu.dto.request.CategoryRequest;
import com.fnb.menu.dto.response.CategoryResponse;
import com.fnb.menu.entity.Category;
import com.fnb.menu.repository.CategoryRepository;
import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    @Cacheable(value = "menu:categories", key = "#page + '_' + #size")
    public PageResponse<CategoryResponse> getActiveCategories(int page, int size) {
        Page<Category> result = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc(
                PageRequest.of(page, size)
        );
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    public PageResponse<CategoryResponse> listForAdmin(String keyword, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            keyword = "";
        }
        Page<Category> result = categoryRepository.findAllForAdmin(
                keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "displayOrder")));
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CategoryResponse getDetail(UUID id) {
        return toResponse(findById(id));
    }

    @Transactional
    @CacheEvict(value = "menu:categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "menu:categories", allEntries = true)
    public CategoryResponse update(UUID id, CategoryRequest request) {
        Category category = findById(id);
        
        // Handle Cloudinary Cleanup
        if (request.getImageUrl() != null && !request.getImageUrl().equals(category.getImageUrl())) {
            if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
                cloudinaryService.deleteImage(category.getImageUrl());
            }
            category.setImageUrl(request.getImageUrl());
        }

        category.setName(request.getName());
        category.setDisplayOrder(request.getDisplayOrder());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = "menu:categories", allEntries = true)
    public void delete(UUID id) {
        Category category = findById(id);
        if (category.getItems() != null && !category.getItems().isEmpty()) {
            long activeCount = category.getItems().stream().filter(i -> i.isActive()).count();
            if (activeCount > 0) {
                throw new BusinessException("Không thể ẩn danh mục đang có " + activeCount + " món còn hoạt động");
            }
        }
        category.setActive(false); // soft delete: giữ lịch sử, menu items không bị mất FK
        categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(value = "menu:categories", allEntries = true)
    public void hardDelete(UUID id) {
        Category category = findById(id);
        if (category.getItems() != null && !category.getItems().isEmpty()) {
            throw new BusinessException("Không thể xóa vĩnh viễn danh mục đang chứa món ăn. Hãy xóa/chuyển món trước.");
        }
        
        // Cleanup image on Cloudinary
        if (category.getImageUrl() != null && !category.getImageUrl().isEmpty()) {
            cloudinaryService.deleteImage(category.getImageUrl());
        }
        
        categoryRepository.delete(category);
    }

    @Transactional
    @CacheEvict(value = "menu:categories", allEntries = true)
    public CategoryResponse toggleStatus(UUID id) {
        Category category = findById(id);
        
        // Nếu người dùng muốn ẨN danh mục đang bật
        if (category.isActive()) {
            if (category.getItems() != null && !category.getItems().isEmpty()) {
                long activeCount = category.getItems().stream().filter(i -> i.isActive()).count();
                if (activeCount > 0) {
                    throw new BusinessException("Không thể ẩn danh mục đang có " + activeCount + " món còn hoạt động. Vui lòng ẩn hết món trước.");
                }
            }
        }
        
        category.setActive(!category.isActive());
        return toResponse(categoryRepository.save(category));
    }

    private Category findById(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
    }

    private CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .imageUrl(c.getImageUrl())
                .displayOrder(c.getDisplayOrder())
                .isActive(c.isActive())
                .build();
    }
}
