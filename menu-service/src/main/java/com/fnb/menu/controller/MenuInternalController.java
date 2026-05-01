package com.fnb.menu.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/menu/internal")
@RequiredArgsConstructor
public class MenuInternalController {

    private final MenuItemRepository menuItemRepository;
    private final com.fnb.menu.service.MenuItemService menuItemService;

    /**
     * Noi bo: order-service goi moi cuoi ngay de update mon ban chay.
     * /api/menu/** da duoc whitelist public o SecurityConfig nen khong can JWT.
     */
    @PutMapping("/featured")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateFeaturedItems(@RequestBody List<UUID> itemIds) {
        menuItemRepository.resetAllFeatured();
        if (itemIds != null && !itemIds.isEmpty()) {
            menuItemRepository.setFeaturedItems(itemIds);
        }
        return ResponseEntity.ok(ApiResponse.ok("Updated featured items", null));
    }

    @PostMapping("/items/bulk")
    public ResponseEntity<ApiResponse<List<com.fnb.menu.dto.response.MenuItemResponse>>> getBulkItems(@RequestBody List<UUID> itemIds) {
        List<com.fnb.menu.dto.response.MenuItemResponse> responses = new java.util.ArrayList<>();
        for (UUID id : itemIds) {
            try {
                responses.add(menuItemService.getItem(id));
            } catch (Exception e) {
                // Bo qua neu mon bi an/xoa trong tich tac
            }
        }
        return ResponseEntity.ok(ApiResponse.ok("Fetched items", responses));
    }

    /**
     * [Phase 1.5] Admin AI: Tong quan trang thai menu.
     * Tra ve: so mon hien co, het hang, dang sale, so danh muc.
     * Duoc goi boi AdminReportTools.getMenuOverview() qua Feign.
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<MenuOverviewResponse>> getMenuOverview() {
        List<com.fnb.menu.entity.MenuItem> allActive = menuItemRepository.findAll().stream()
                .filter(m -> m.isActive())
                .toList();

        long totalActive  = allActive.size();
        long unavailable  = allActive.stream().filter(m -> !m.isAvailable()).count();
        LocalDateTime now = LocalDateTime.now();
        long onSale       = allActive.stream()
                .filter(m -> m.getSalePrice() != null
                        && (m.getSaleStartAt() == null || !now.isBefore(m.getSaleStartAt()))
                        && (m.getSaleEndAt()   == null || !now.isAfter(m.getSaleEndAt())))
                .count();
        long featured     = allActive.stream().filter(com.fnb.menu.entity.MenuItem::isFeatured).count();

        // Mon het hang theo tram (station)
        java.util.Map<String, Long> unavailableByStation = allActive.stream()
                .filter(m -> !m.isAvailable())
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getStation() != null ? m.getStation() : "UNKNOWN",
                        java.util.stream.Collectors.counting()
                ));

        // Mon het hang cu the (de AI biet chinh xac)
        List<UnavailableItem> unavailableItems = allActive.stream()
                .filter(m -> !m.isAvailable())
                .limit(20)
                .map(m -> new UnavailableItem(
                        m.getName(),
                        m.getStation() != null ? m.getStation() : "UNKNOWN",
                        m.getBasePrice()
                ))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok("Menu overview", new MenuOverviewResponse(
                totalActive, unavailable, onSale, featured,
                unavailableByStation, unavailableItems
        )));
    }

    public record MenuOverviewResponse(
            long totalActiveItems,
            long unavailableItems,
            long itemsOnSale,
            long featuredItems,
            java.util.Map<String, Long> unavailableByStation,
            List<UnavailableItem> unavailableItemList
    ) {}

    public record UnavailableItem(
            String name,
            String station,
            BigDecimal basePrice
    ) {}
}
