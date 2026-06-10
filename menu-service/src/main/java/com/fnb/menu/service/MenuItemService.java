package com.fnb.menu.service;

import com.fnb.menu.dto.request.MenuItemRequest;
import com.fnb.menu.dto.response.MenuItemResponse;
import com.fnb.menu.entity.*;
import com.fnb.menu.repository.CategoryRepository;
import com.fnb.menu.repository.ItemOptionRepository;
import com.fnb.menu.repository.MenuItemRepository;
import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import com.fnb.menu.dto.response.PromotionResponse;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.fnb.common.util.PricingEngine;
import com.fnb.menu.dto.event.MenuUpdatedEvent;
import com.fnb.menu.entity.Promotion;
import com.fnb.menu.repository.PromotionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final ItemOptionRepository itemOptionRepository;
    private final CloudinaryService cloudinaryService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final StringRedisTemplate stringRedisTemplate;

    private final PromotionRepository promotionRepository;

    // ─── Public: customer đọc ────────────────────────────────────────────

    @Cacheable(value = "menu:items", key = "#categoryId != null ? #categoryId.toString() + '_' + #page + '_' + #size : 'all_' + #page + '_' + #size")
    public PageResponse<MenuItemResponse> getItemsByCategory(UUID categoryId, int page, int size) {
        Page<MenuItem> result = (categoryId != null)
                ? menuItemRepository.findByCategoryPublic(categoryId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                : menuItemRepository.findAllActivePublic(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Cacheable(value = "menu:items_search", key = "#categoryId + '_' + #keyword + '_' + #maxPrice + '_' + #page + '_' + #size")
    public PageResponse<MenuItemResponse> searchForCustomer(UUID categoryId, String keyword, BigDecimal maxPrice, int page, int size) {
        if (keyword == null) keyword = "";
        Page<MenuItem> result = menuItemRepository.searchForCustomer(
                categoryId, keyword, maxPrice,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    @Cacheable(value = "menu:item", key = "#id")
    public MenuItemResponse getItem(UUID id) {
        MenuItem item = menuItemRepository.findByIdWithOptions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));
        return toResponse(item);
    }

    public PageResponse<MenuItemResponse> getFeatured(int page, int size) {
        Page<MenuItem> result = menuItemRepository.findByIsAvailableTrueAndIsActiveTrueAndIsFeaturedTrue(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
                
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }

    // ─── Admin: CRUD ──────────────────────────────────────────────────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:items", key = "#request.categoryId"),
        @CacheEvict(value = "menu:categories", allEntries = true)
    })
    public MenuItemResponse create(MenuItemRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));

        MenuItem item = MenuItem.builder()
                .category(category)
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .basePrice(request.getBasePrice())
                .station(request.getStation())
                .isFeatured(request.isFeatured())
                .isAvailable(request.isAvailable())
                .taxRate(request.getTaxRate())
                .build();

        if (request.getOptionGroups() != null) {
            request.getOptionGroups().forEach(groupReq -> {
                ItemOptionGroup group = ItemOptionGroup.builder()
                        .item(item)
                        .name(groupReq.getName())
                        .type(groupReq.getType())
                        .isRequired(groupReq.isRequired())
                        .displayOrder(groupReq.getDisplayOrder())
                        .options(new ArrayList<>())
                        .build();

                if (groupReq.getOptions() != null) {
                    groupReq.getOptions().forEach(optReq ->
                        group.getOptions().add(ItemOption.builder()
                                .group(group)
                                .name(optReq.getName())
                                .extraPrice(optReq.getExtraPrice())
                                .build())
                    );
                }
                item.getOptionGroups().add(group);
            });
        }

        MenuItem savedItem = menuItemRepository.save(item);
        try {
            stringRedisTemplate.convertAndSend("ai-menu-sync-topic", savedItem.getId().toString());
        } catch (Exception e) {
            log.error("Lỗi khi publish sự kiện đồng bộ AI: {}", e.getMessage());
        }
        return toResponse(savedItem);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public MenuItemResponse update(UUID id, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findByIdWithOptions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Danh mục không tồn tại"));
            item.setCategory(category);
        }

        item.setName(request.getName());
        if (request.getDescription() != null) item.setDescription(request.getDescription());

        // Capture old URL trước khi ghi đè
        String oldImageUrl = item.getImageUrl();
        String newImageUrl = request.getImageUrl();

        // So sánh để quyết định có update ảnh hay không
        boolean isChanged = (newImageUrl == null && oldImageUrl != null) || 
                           (newImageUrl != null && !newImageUrl.equals(oldImageUrl));

        if (isChanged) {
            item.setImageUrl(newImageUrl);
            // Chỉ cleanup ảnh cũ SAU khi transaction commit thành công
            if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        cloudinaryService.deleteImage(oldImageUrl);
                    }
                });
            }
        }

        item.setBasePrice(request.getBasePrice());
        item.setStation(request.getStation());
        item.setFeatured(request.isFeatured());
        item.setAvailable(request.isAvailable());
        item.setTaxRate(request.getTaxRate());

        if (request.getOptionGroups() != null) {
            item.getOptionGroups().clear();
            request.getOptionGroups().forEach(groupReq -> {
                ItemOptionGroup group = ItemOptionGroup.builder()
                        .item(item)
                        .name(groupReq.getName())
                        .type(groupReq.getType())
                        .isRequired(groupReq.isRequired())
                        .displayOrder(groupReq.getDisplayOrder())
                        .options(new ArrayList<>())
                        .build();

                if (groupReq.getOptions() != null) {
                    groupReq.getOptions().forEach(optReq ->
                        group.getOptions().add(ItemOption.builder()
                                .group(group)
                                .name(optReq.getName())
                                .extraPrice(optReq.getExtraPrice())
                                .build())
                    );
                }
                item.getOptionGroups().add(group);
            });
        }

        MenuItem savedItem = menuItemRepository.save(item);
        try {
            stringRedisTemplate.convertAndSend("ai-menu-sync-topic", savedItem.getId().toString());
        } catch (Exception e) {
            log.error("Lỗi khi publish sự kiện đồng bộ AI: {}", e.getMessage());
        }
        return toResponse(savedItem);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public MenuItemResponse toggleAvailability(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));
        item.setAvailable(!item.isAvailable());
        MenuItem saved = menuItemRepository.save(item);
        
        applicationEventPublisher.publishEvent(MenuUpdatedEvent.builder()
                .itemId(saved.getId())
                .type("ITEM")
                .isAvailable(saved.isAvailable())
                .isActive(saved.isActive())
                .updatedAt(LocalDateTime.now())
                .build());
                
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public void delete(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));
        item.setAvailable(false);
        item.setActive(false);  // soft delete đầy đủ: ẩn khỏi mọi query lọc theo isActive
        menuItemRepository.save(item);
        
        applicationEventPublisher.publishEvent(MenuUpdatedEvent.builder()
                .itemId(item.getId())
                .type("ITEM")
                .isAvailable(false)
                .isActive(false)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public MenuItemResponse restore(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));
        item.setAvailable(true);
        item.setActive(true);
        MenuItem saved = menuItemRepository.save(item);
        
        applicationEventPublisher.publishEvent(MenuUpdatedEvent.builder()
                .itemId(saved.getId())
                .type("ITEM")
                .isAvailable(true)
                .isActive(true)
                .updatedAt(LocalDateTime.now())
                .build());
                
        return toResponse(saved);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public void hardDelete(UUID id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));

        String imageUrl = item.getImageUrl();
        menuItemRepository.delete(item);

        // Cleanup SAU khi DB đã xóa thành công (fire-and-forget, không block)
        if (imageUrl != null && !imageUrl.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cloudinaryService.deleteImage(imageUrl);
                }
            });
        }
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#id"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public MenuItemResponse addOptions(UUID id, List<MenuItemRequest.OptionGroupRequest> groups) {
        MenuItem item = menuItemRepository.findByIdWithOptions(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại"));

        item.getOptionGroups().clear();

        groups.forEach(groupReq -> {
            ItemOptionGroup group = ItemOptionGroup.builder()
                    .item(item)
                    .name(groupReq.getName())
                    .type(groupReq.getType())
                    .isRequired(groupReq.isRequired())
                    .displayOrder(groupReq.getDisplayOrder())
                    .options(new ArrayList<>())
                    .build();

            if (groupReq.getOptions() != null) {
                groupReq.getOptions().forEach(optReq ->
                    group.getOptions().add(ItemOption.builder()
                            .group(group)
                            .name(optReq.getName())
                            .extraPrice(optReq.getExtraPrice())
                            .build())
                );
            }
            item.getOptionGroups().add(group);
        });

        return toResponse(menuItemRepository.save(item));
    }

    // ─── Admin: list tất cả món (cả ẩn) có filter + keyword + pagination ─────────

    public PageResponse<MenuItemResponse> listForAdmin(UUID categoryId, Boolean isActive, Boolean isAvailable, Boolean isFeatured, String station, String keyword, int page, int size) {
        // Ép về chuỗi rỗng nếu null, chặn đứng nguy cơ lỗi ở DB
        if (keyword == null || keyword.isBlank()) {
            keyword = "";
        }

        if (station != null && station.isBlank()) {
            station = null;
        }

        Page<MenuItem> result = menuItemRepository.findAllForAdmin(
                categoryId, isActive, isAvailable, isFeatured, station, keyword,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements()
        );
    }
    // ─── Admin: toggle từng option riêng lẻ (bếp hết topping) ──────────

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "menu:item", key = "#itemId"),
        @CacheEvict(value = "menu:items", allEntries = true)
    })
    public MenuItemResponse toggleOption(UUID itemId, UUID optionId) {
        ItemOption option = itemOptionRepository.findByIdAndItemId(optionId, itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Option không tồn tại hoặc không thuộc món này"));
        option.setAvailable(!option.isAvailable());
        itemOptionRepository.save(option);
        
        applicationEventPublisher.publishEvent(MenuUpdatedEvent.builder()
                .itemId(itemId)
                .optionId(optionId)
                .type("OPTION")
                .isAvailable(option.isAvailable())
                .updatedAt(LocalDateTime.now())
                .build());
                
        // reload full item với options để trả về response đầy đủ
        return getItem(itemId);
    }

    // ─── Mapper ──────────────────────────────────────────────────────────

    private MenuItemResponse toResponse(MenuItem item) {
        List<MenuItemResponse.OptionGroupResponse> groups = item.getOptionGroups().stream()
                .map(g -> MenuItemResponse.OptionGroupResponse.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .type(g.getType())
                        .isRequired(g.isRequired())
                        .displayOrder(g.getDisplayOrder())
                        .options(g.getOptions().stream()
                                .map(o -> MenuItemResponse.OptionResponse.builder()
                                        .id(o.getId())
                                        .name(o.getName())
                                        .extraPrice(o.getExtraPrice())
                                        .isAvailable(o.isAvailable())
                                        .build())
                                .toList())
                        .build())
                .toList();

        // --- Xử lý Stateless Dynamic Pricing ---
        BigDecimal finalSalePrice = null;
        LocalDateTime startAt = null;
        LocalDateTime endAt = null;
        List<PromotionResponse.ScheduleResponse> schedules = null;
        
        try {
            LocalDateTime now = LocalDateTime.now();
            List<Promotion> allProductPromos = promotionRepository.findActiveByScope("PRODUCT", now);
            
            // Lọc theo khung giờ lặp lại (Happy Hour)
            List<Promotion> productPromos = allProductPromos.stream()
                .filter(p -> isWithinSchedule(p, now))
                .toList();

            List<Promotion> itemPromos = productPromos.stream()
                .filter(p -> PricingEngine.isApplicable(p.getTargets(), item.getId(), item.getCategory().getId()))
                .toList();

            if (!itemPromos.isEmpty()) {
                BigDecimal basePrice = item.getBasePrice() != null ? item.getBasePrice() : BigDecimal.ZERO;
                
                var bestResult = PricingEngine.selectBestPromotion(itemPromos, basePrice);
                Promotion bestPromo = bestResult.getPromotion();
                BigDecimal bestDiscount = bestResult.getDiscountAmount();

                if (bestPromo != null && bestDiscount.compareTo(BigDecimal.ZERO) >= 0) {
                    finalSalePrice = basePrice.subtract(bestDiscount).max(BigDecimal.ZERO);
                    startAt = bestPromo.getStartAt();
                    endAt = bestPromo.getEndAt();
                    schedules = bestPromo.getSchedules().stream()
                        .map(s -> PromotionResponse.ScheduleResponse.builder()
                            .id(s.getId()).dayOfWeek(s.getDayOfWeek())
                            .startTime(s.getStartTime()).endTime(s.getEndTime())
                            .build())
                        .toList();
                }
            }
        } catch (Exception e) {
            log.error("Failed to map dynamic pricing for item {}: {}", item.getId(), e.getMessage());
        }

        return MenuItemResponse.builder()
                .id(item.getId())
                .categoryId(item.getCategory() != null ? item.getCategory().getId() : null)
                .categoryName(item.getCategory() != null ? item.getCategory().getName() : null)
                .name(item.getName())
                .description(item.getDescription())
                .imageUrl(item.getImageUrl())
                .basePrice(item.getBasePrice())
                .taxRate(item.getTaxRate() != null ? item.getTaxRate() : 
                         (item.getCategory() != null && item.getCategory().getTaxRate() != null ? item.getCategory().getTaxRate() : new BigDecimal("0.00")))
                .salePrice(finalSalePrice)
                .saleStartAt(startAt)
                .saleEndAt(endAt)
                .schedules(schedules)
                .station(item.getStation())
                .isAvailable(item.isAvailable())
                .isFeatured(item.isFeatured())
                .isActive(item.isActive())
                .optionGroups(groups)
                .build();
    }

    private boolean isWithinSchedule(Promotion promotion, LocalDateTime now) {
        if (promotion.getSchedules() == null || promotion.getSchedules().isEmpty()) return true;
        int todayDow = now.getDayOfWeek().getValue() % 7; // Mon=1, Sun=7 -> map Sun=0
        return promotion.getSchedules().stream().anyMatch(s ->
                s.getDayOfWeek() == todayDow
                        && !now.toLocalTime().isBefore(s.getStartTime())
                        && !now.toLocalTime().isAfter(s.getEndTime()));
    }
}
