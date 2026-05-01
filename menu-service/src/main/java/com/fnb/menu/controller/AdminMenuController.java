package com.fnb.menu.controller;

import com.fnb.menu.dto.request.CategoryRequest;
import com.fnb.menu.dto.request.MenuItemRequest;
import com.fnb.menu.dto.response.CategoryResponse;
import com.fnb.menu.dto.response.MenuItemResponse;
import com.fnb.menu.service.CategoryService;
import com.fnb.menu.service.MenuItemService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin-only menu management.
 * Yêu cầu role ADMIN — được enforce ở SecurityConfig + @PreAuthorize.
 */
@RestController
@RequestMapping("/api/admin/menu")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMenuController {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;

    // ─── Categories ───────────────────────────────────────────────────────

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getAllCategories(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.listForAdmin(keyword, page, size)));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo danh mục thành công", categoryService.create(request)));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", categoryService.update(id, request)));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Ẩn danh mục thành công", null));
    }

    @DeleteMapping("/categories/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteCategory(@PathVariable UUID id) {
        categoryService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa vĩnh viễn danh mục thành công", null));
    }

    @PatchMapping("/categories/{id}/toggle")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategoryStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", categoryService.toggleStatus(id)));
    }

    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getDetail(id)));
    }

    // ─── Menu Items ──────────────────────────────────────────────────

    // GET /api/admin/menu/items?categoryId=&isActive=&keyword=&page=0&size=20
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> listItems(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean isAvailable,
            @RequestParam(required = false) Boolean isFeatured,
            @RequestParam(required = false) String station,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.listForAdmin(categoryId, isActive, isAvailable, isFeatured, station, keyword, page, size)));
    }

    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItemDetail(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.getItem(id)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<MenuItemResponse>> createItem(
            @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo món thành công", menuItemService.create(request)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> updateItem(
            @PathVariable UUID id, @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", menuItemService.update(id, request)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteItem(@PathVariable UUID id) {
        menuItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Ẩn món thành công", null));
    }

    @PatchMapping("/items/{id}/restore")
    public ResponseEntity<ApiResponse<MenuItemResponse>> restoreItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Khôi phục món thành công", menuItemService.restore(id)));
    }

    @DeleteMapping("/items/{id}/hard")
    public ResponseEntity<ApiResponse<Void>> hardDeleteItem(@PathVariable UUID id) {
        menuItemService.hardDelete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa vĩnh viễn thành công", null));
    }

    @PatchMapping("/items/{id}/toggle")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.toggleAvailability(id)));
    }

    // PATCH /api/admin/menu/items/{id}/options/{optionId}/toggle
    @PatchMapping("/items/{id}/options/{optionId}/toggle")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleOption(
            @PathVariable UUID id,
            @PathVariable UUID optionId) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật option thành công", menuItemService.toggleOption(id, optionId)));
    }

    @PostMapping("/items/{id}/options")
    public ResponseEntity<ApiResponse<MenuItemResponse>> addOptions(
            @PathVariable UUID id,
            @Valid @RequestBody List<MenuItemRequest.OptionGroupRequest> groups) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật options thành công", menuItemService.addOptions(id, groups)));
    }

}
