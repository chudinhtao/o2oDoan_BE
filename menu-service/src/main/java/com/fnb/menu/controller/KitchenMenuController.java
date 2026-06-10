package com.fnb.menu.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.menu.dto.response.MenuItemResponse;
import com.fnb.menu.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/menu/kds")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN')")
public class KitchenMenuController {

    private final MenuItemService menuItemService;

    @PatchMapping("/items/{id}/toggle")
    public ResponseEntity<ApiResponse<MenuItemResponse>> toggleItem(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.toggleAvailability(id)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<PageResponse<MenuItemResponse>>> listItemsForKds(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Kitchen KDS needs to see active items regardless of isAvailable status so they can toggle them
        return ResponseEntity.ok(ApiResponse.ok(menuItemService.listForAdmin(null, true, null, null, null, keyword, page, size)));
    }
}
