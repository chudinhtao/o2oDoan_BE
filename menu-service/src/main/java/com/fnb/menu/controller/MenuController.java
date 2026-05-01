package com.fnb.menu.controller;

import com.fnb.menu.dto.response.CategoryResponse;
import com.fnb.menu.dto.response.MenuItemResponse;
import com.fnb.menu.service.CategoryService;
import com.fnb.menu.service.MenuItemService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * PUBLIC endpoints — không cần JWT (khách hàng đọc menu).
 * Gateway đã cấu hình /api/menu/** là public.
 */
@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
public class MenuController {

    private final CategoryService categoryService;
    private final MenuItemService menuItemService;

    // GET /api/menu/categories — danh mục active
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<PageResponse<CategoryResponse>>> getCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getActiveCategories(page, size)));
    }

    // GET /api/menu/items?categoryId=&featured=&keyword=&maxPrice=
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> getItems(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "false") boolean featured,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        PageResponse<MenuItemResponse> result;
        if (featured) {
            result = menuItemService.getFeatured(page, size);
        } else if ((keyword != null && !keyword.isBlank()) || maxPrice != null) {
            result = menuItemService.searchForCustomer(categoryId, keyword, maxPrice, page, size);
        } else {
            result = menuItemService.getItemsByCategory(categoryId, page, size);
        }

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // GET /api/menu/items/{id} — chi tiết + options
    @GetMapping("/items/{id}")
    public ResponseEntity<ApiResponse<MenuItemResponse>> getItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.getItem(id)));
    }
}
