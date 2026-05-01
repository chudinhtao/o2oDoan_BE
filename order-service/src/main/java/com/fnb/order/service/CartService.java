package com.fnb.order.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.common.exception.BusinessException;
import com.fnb.order.client.MenuServiceClient;
import com.fnb.order.dto.event.CartUpdatedEvent;
import com.fnb.order.dto.redis.*;
import com.fnb.order.dto.request.TicketItemOptionRequest;
import com.fnb.order.dto.request.TicketItemRequest;
import com.fnb.order.dto.response.SessionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SessionService sessionService;
    private final MenuServiceClient menuClient;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final String CART_PREFIX = "cart:session:";
    private static final long CART_TTL_HOURS = 4;

    /**
     * Lấy giỏ hàng thao tác trên Redis
     */
    public CartDto getCart(String sessionToken) {
        String key = CART_PREFIX + sessionToken;
        String cartJson = redisTemplate.opsForValue().get(key);
        CartDto cart = null;

        if (cartJson != null) {
            try {
                cart = objectMapper.readValue(cartJson, CartDto.class);
            } catch (JsonProcessingException e) {
                log.error("Lỗi parse JSON giỏ hàng: {}", e.getMessage());
            }
        }

        if (cart == null) {
            cart = new CartDto();
            cart.setSessionToken(sessionToken);
        }

        applyFlashSalePricing(cart);
        cart.recalculateTotal();
        return cart;
    }

    private void applyFlashSalePricing(CartDto cart) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            cart.setAutomatedDiscount(BigDecimal.ZERO);
            cart.setAppliedPromotions(new ArrayList<>());
            return;
        }

        cart.getAppliedPromotions().clear();
        BigDecimal totalAutoDiscount = BigDecimal.ZERO;
        
        try {
            var activePromosRes = menuClient.getActivePromotions();
            if (activePromosRes != null && activePromosRes.isSuccess() && activePromosRes.getData() != null) {
                List<MenuServiceClient.PromotionDetail> allPromos = activePromosRes.getData();
                
                // --- 1. Level 0: Bundle Matching (Combo) ---
                List<MenuServiceClient.PromotionDetail> bundleRules = allPromos.stream()
                        .filter(p -> "BUNDLE".equals(p.scope()) && "AUTO".equals(p.triggerType()))
                        .toList();

                Map<UUID, Integer> cartMap = new HashMap<>();
                Map<UUID, BigDecimal> priceMap = new HashMap<>();
                Map<UUID, BigDecimal> flashSaleBenefitMap = new HashMap<>(); // Ước tính mức giảm nếu dùng Flash Sale

                // Lọc trước các Flash Sale để so sánh "Cơ hội"
                var productAutoPromos = allPromos.stream()
                        .filter(p -> "AUTO".equals(p.triggerType()) && "PRODUCT".equals(p.scope()))
                        .toList();

                // Pre-fetch all item categories for efficient filtering
                Map<UUID, UUID> itemCategoryMap = new HashMap<>();
                for (CartItemDto item : cart.getItems()) {
                    try {
                        var res = menuClient.getMenuItemById(item.getMenuItemId());
                        if (res != null && res.isSuccess() && res.getData() != null) {
                            itemCategoryMap.put(item.getMenuItemId(), res.getData().categoryId());
                            // Fix missing image for existing cart items in Redis
                            if (item.getImageUrl() == null || item.getImageUrl().isBlank()) {
                                item.setImageUrl(res.getData().imageUrl());
                            }
                        }
                    } catch (Exception e) {}
                }

                for (CartItemDto item : cart.getItems()) {
                    UUID itemId = item.getMenuItemId();
                    UUID categoryId = itemCategoryMap.get(itemId);
                    cartMap.put(itemId, cartMap.getOrDefault(itemId, 0) + item.getQuantity());
                    priceMap.put(itemId, item.getUnitPrice());
                    
                    var itemPromos = productAutoPromos.stream()
                            .filter(p -> com.fnb.common.util.PricingEngine.isApplicable(p.targets(), itemId, categoryId))
                            .toList();
                    
                    if (!itemPromos.isEmpty()) {
                        var bestFlash = com.fnb.common.util.PricingEngine.selectBestPromotion(itemPromos, item.getUnitPrice());
                        flashSaleBenefitMap.put(itemId, bestFlash.getDiscountAmount());
                    } else {
                        flashSaleBenefitMap.put(itemId, BigDecimal.ZERO);
                    }
                }

                if (!bundleRules.isEmpty()) {
                    log.info("Tìm thấy {} quy tắc Combo khả thi", bundleRules.size());
                    List<MenuServiceClient.PromotionDetail> profitableBundles = bundleRules.stream().filter(rule -> {
                        BigDecimal totalFlashBenefit = BigDecimal.ZERO;
                        for (var bi : rule.getBundleItems()) {
                            BigDecimal perItemFlash = flashSaleBenefitMap.getOrDefault(bi.getItemId(), BigDecimal.ZERO);
                            totalFlashBenefit = totalFlashBenefit.add(perItemFlash.multiply(BigDecimal.valueOf(bi.getQuantity())));
                        }
                        BigDecimal bBase = BigDecimal.ZERO;
                        int itemsFound = 0;
                        for (var bi : rule.getBundleItems()) {
                            BigDecimal p = priceMap.get(bi.getItemId());
                            if (p != null) {
                                bBase = bBase.add(p.multiply(BigDecimal.valueOf(bi.getQuantity())));
                                itemsFound++;
                            }
                        }
                        BigDecimal bundleDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                                bBase, rule.getDiscountType(), rule.getDiscountValue(), rule.getMaxDiscount());
                                
                        log.info("Check Combo '{}': Tìm thấy {}/{} món, Tổng gốc={}, Loại={}, Giá trị={}, Mức giảm={}", 
                                rule.getName(), itemsFound, rule.getBundleItems().size(), bBase, rule.getDiscountType(), rule.getDiscountValue(), bundleDiscount);

                        boolean isProfitable = bundleDiscount.compareTo(totalFlashBenefit) >= 0;
                        if (!isProfitable) {
                            log.info("Combo '{}' bị bỏ qua vì ưu tiên Flash Sale lẻ (Lợi ích: {} < {})", 
                                    rule.getName(), bundleDiscount, totalFlashBenefit);
                        }
                        return isProfitable;
                    }).toList();

                    if (!profitableBundles.isEmpty()) {
                        var matchedBundles = com.fnb.common.util.BundleMatcher.matchBundles(cartMap, profitableBundles, priceMap);
                        for (var res : matchedBundles) {
                            log.info("Đã khớp Combo: {} x{}", res.getRule().getName(), res.getCount());
                            totalAutoDiscount = totalAutoDiscount.add(res.getTotalDiscount());
                            cart.getAppliedPromotions().add(res.getRule().getName());
                        }
                    } else {
                        log.info("Không có Combo nào có lợi hơn mua lẻ (Flash Sale)");
                    }
                }

                // --- 2. Level 1: Product Auto Discount (Flash Sale) ---
                for (CartItemDto item : cart.getItems()) {
                    item.setHasFlashSale(false);
                    item.setDiscountPrice(null);

                    int remainingQty = cartMap.getOrDefault(item.getMenuItemId(), 0);
                    if (remainingQty <= 0) continue;

                    UUID itemId = item.getMenuItemId();
                    UUID categoryId = itemCategoryMap.get(itemId);
                    var itemPromos = productAutoPromos.stream()
                            .filter(p -> com.fnb.common.util.PricingEngine.isApplicable(p.targets(), itemId, categoryId))
                            .toList();

                    if (!itemPromos.isEmpty()) {
                        BigDecimal basePrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                        var bestResult = com.fnb.common.util.PricingEngine.selectBestPromotion(itemPromos, basePrice);
                        if (bestResult.getPromotion() != null) {
                            totalAutoDiscount = totalAutoDiscount.add(bestResult.getDiscountAmount().multiply(BigDecimal.valueOf(remainingQty)));
                            item.setHasFlashSale(true);
                            item.setDiscountPrice(basePrice.subtract(bestResult.getDiscountAmount()).max(BigDecimal.ZERO));
                            item.setSaleEndAt(bestResult.getPromotion().getEndAt());
                            cartMap.put(item.getMenuItemId(), 0);
                            cart.getAppliedPromotions().add(bestResult.getPromotion().getName());
                        }
                    }
                }

                // --- 3. Level 2: Order Auto Discount (Happy Hour) ---
                BigDecimal subtotalAfterL0L1 = cart.getOriginalTotal().subtract(totalAutoDiscount);
                var orderAutoPromos = allPromos.stream()
                        .filter(p -> "ORDER".equals(p.scope()) && "AUTO".equals(p.triggerType()))
                        .filter(p -> p.requirement() == null || subtotalAfterL0L1.compareTo(p.requirement().minOrderAmount()) >= 0)
                        .toList();

                if (!orderAutoPromos.isEmpty()) {
                    var bestResult = com.fnb.common.util.PricingEngine.selectBestPromotion(orderAutoPromos, subtotalAfterL0L1);
                    if (bestResult.getPromotion() != null) {
                        if (Boolean.FALSE.equals(bestResult.getPromotion().stackable())) {
                            BigDecimal standAloneDiscount = com.fnb.common.util.PricingEngine.calculateRawDiscount(
                                cart.getOriginalTotal(), bestResult.getPromotion().discountType(), 
                                bestResult.getPromotion().discountValue(), bestResult.getPromotion().maxDiscount()
                            );
                            if (standAloneDiscount.compareTo(totalAutoDiscount) > 0) {
                                totalAutoDiscount = standAloneDiscount;
                                cart.getAppliedPromotions().clear();
                                cart.getAppliedPromotions().add(bestResult.getPromotion().getName());
                                // Xóa discount của item vì bị override
                                for (CartItemDto item : cart.getItems()) {
                                    item.setHasFlashSale(false);
                                    item.setDiscountPrice(null);
                                }
                            }
                        } else {
                            totalAutoDiscount = totalAutoDiscount.add(bestResult.getDiscountAmount());
                            cart.getAppliedPromotions().add(bestResult.getPromotion().getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Lỗi áp dụng Pricing Engine cho Giỏ hàng: {}", e.getMessage(), e);
        }
        
        cart.setAutomatedDiscount(totalAutoDiscount);
    }

    /**
     * Helper thực hiện Redis Distributed Spin Lock
     */
    private <T> T executeWithLock(String sessionToken, Supplier<T> action) {
        String lockKey = "lock:cart:" + sessionToken;
        int retries = 0;
        while (retries < 15) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 3, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(acquired)) {
                try {
                    return action.get();
                } finally {
                    redisTemplate.delete(lockKey);
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BusinessException("Lỗi hệ thống khi cập nhật giỏ hàng.");
            }
            retries++;
        }
        throw new BusinessException("Hệ thống đang xử lý yêu cầu của người cùng bàn. Vui lòng thử lại sau giây lát.");
    }

    private void saveCart(CartDto cart) {
        try {
            cart.recalculateTotal();
            String key = CART_PREFIX + cart.getSessionToken();
            String json = objectMapper.writeValueAsString(cart);
            redisTemplate.opsForValue().set(key, json, CART_TTL_HOURS, TimeUnit.HOURS);

            // Lấy Session để biết bàn số mấy
            SessionResponse session = sessionService
                    .getSessionCurrent(cart.getSessionToken());

            CartUpdatedEvent event = CartUpdatedEvent.builder()
                    .sessionToken(cart.getSessionToken())
                    .tableNumber(session.getTableNumber())
                    .build();
            applicationEventPublisher.publishEvent(event);

        } catch (JsonProcessingException e) {
            log.error("Lỗi serialize giỏ hàng: {}", e.getMessage());
        }
    }

    /**
     * Thêm món tủ vào giỏ hàng
     */
    public CartDto addItemToCart(String sessionToken, TicketItemRequest request) {
        return executeWithLock(sessionToken, () -> {
            var session = sessionService.getSessionCurrent(sessionToken);
            if (!"ACTIVE".equals(session.getStatus())) {
                throw new BusinessException("Phiên làm việc đã kết thúc hoặc không hợp lệ, không thể thêm món.");
            }
            MenuServiceClient.MenuItemDetail validation = fetchMenuValidation(request.getMenuItemId());
            CartDto cart = getCart(sessionToken);

            CartItemDto newItem = new CartItemDto();
            newItem.setCartItemId(UUID.randomUUID().toString());
            newItem.setMenuItemId(request.getMenuItemId());
            newItem.setQuantity(request.getQuantity());
            newItem.setNote(request.getNote());

            newItem.setItemName(validation.name());
            // Giá gốc — Pricing Engine sẽ tính discount động khi checkout
            newItem.setUnitPrice(validation.basePrice());
            newItem.setStation(validation.station());
            newItem.setImageUrl(validation.imageUrl());

            if (request.getOptions() != null && !request.getOptions().isEmpty()) {
                var optionDtos = request.getOptions().stream().map(optReq -> {
                    var dto = new CartItemOptionDto();

                    if (validation.optionGroups() == null) {
                        throw new BusinessException("Món này không có tùy chọn nào, nhưng bạn lại chọn món thêm.");
                    }

                    MenuServiceClient.OptionDetail matchedOption = null;
                    for (var group : validation.optionGroups()) {
                        if (group.options() != null) {
                            for (var opt : group.options()) {
                                if (opt.id().equals(optReq.getOptionId())) {
                                    matchedOption = opt;
                                    break;
                                }
                            }
                        }
                        if (matchedOption != null)
                            break;
                    }

                    if (matchedOption == null) {
                        throw new BusinessException("Tùy chọn không tồn tại hoặc đã bị xóa!");
                    }

                    if (matchedOption.isAvailable() != null && !matchedOption.isAvailable()) {
                        throw new BusinessException(
                                "Tùy chọn " + matchedOption.name() + " hiện đang tạm ngưng kinh doanh.");
                    }

                    dto.setOptionId(optReq.getOptionId());
                    dto.setOptionName(matchedOption.name());
                    dto.setExtraPrice(matchedOption.extraPrice() != null ? matchedOption.extraPrice()
                            : BigDecimal.ZERO);
                    return dto;
                }).collect(Collectors.toList());

                newItem.setOptions(optionDtos);
            }

            // --- Logic gộp món ---
            Optional<CartItemDto> existingItem = cart.getItems().stream()
                    .filter(item -> item.getMenuItemId().equals(newItem.getMenuItemId()))
                    .filter(item -> Objects.equals(item.getNote(), newItem.getNote()))
                    .filter(item -> isSameOptions(item.getOptions(), newItem.getOptions()))
                    .findFirst();

            if (existingItem.isPresent()) {
                log.info("Gộp món trùng trong giỏ hàng: {}", newItem.getItemName());
                existingItem.get().setQuantity(existingItem.get().getQuantity() + newItem.getQuantity());
            } else {
                cart.getItems().add(newItem);
            }

            saveCart(cart);
            return cart;
        });
    }

    private boolean isSameOptions(List<CartItemOptionDto> opts1,
            List<CartItemOptionDto> opts2) {
        if (opts1 == null && opts2 == null)
            return true;
        if (opts1 == null || opts2 == null)
            return false;
        if (opts1.size() != opts2.size())
            return false;

        Set<UUID> ids1 = opts1.stream().map(CartItemOptionDto::getOptionId)
                .collect(Collectors.toSet());
        Set<UUID> ids2 = opts2.stream().map(CartItemOptionDto::getOptionId)
                .collect(Collectors.toSet());

        return ids1.equals(ids2);
    }

    /**
     * Xóa 1 món ra khỏi giỏ
     */
    public CartDto removeItemFromCart(String sessionToken, String cartItemId) {
        return executeWithLock(sessionToken, () -> {
            var session = sessionService.getSessionCurrent(sessionToken);
            if (!"ACTIVE".equals(session.getStatus())) {
                throw new BusinessException("Phiên làm việc đã kết thúc, không thể xóa món.");
            }
            CartDto cart = getCart(sessionToken);
            cart.getItems().removeIf(item -> item.getCartItemId().equals(cartItemId));
            saveCart(cart);
            return cart;
        });
    }

    /**
     * Thay đổi số lượng + note
     */
    public CartDto updateItemQuantity(String sessionToken, String cartItemId, int newQty, String newNote) {
        if (newQty <= 0) {
            return removeItemFromCart(sessionToken, cartItemId);
        }

        return executeWithLock(sessionToken, () -> {
            var session = sessionService.getSessionCurrent(sessionToken);
            if (!"ACTIVE".equals(session.getStatus())) {
                throw new BusinessException("Phiên làm việc đã kết thúc, không thể cập nhật số lượng.");
            }
            CartDto cart = getCart(sessionToken);
            for (CartItemDto item : cart.getItems()) {
                if (item.getCartItemId().equals(cartItemId)) {
                    item.setQuantity(newQty);
                    if (newNote != null)
                        item.setNote(newNote);
                    break;
                }
            }
            saveCart(cart);
            return cart;
        });
    }

    /**
     * Xóa sạch giỏ hàng (khi đã đặt đơn thành công)
     */
    public void clearCart(String sessionToken) {
        String key = CART_PREFIX + sessionToken;
        redisTemplate.delete(key);
    }

    private MenuServiceClient.MenuItemDetail fetchMenuValidation(UUID menuItemId) {
        try {
            var res = menuClient.getMenuItemById(menuItemId);
            if (!res.isSuccess() || res.getData() == null) {
                throw new BusinessException("Món ăn không tồn tại hoặc lỗi đồng bộ menu");
            }
            if (res.getData().isActive() != null && !res.getData().isActive() ||
                    res.getData().isAvailable() != null && !res.getData().isAvailable()) {
                throw new BusinessException(
                        "Rất tiếc, món " + res.getData().name() + " vừa hết hàng hoặc ngưng phục vụ.");
            }
            return res.getData();
        } catch (Exception e) {
            throw new BusinessException("Không thể gọi API xác thực Menu lúc này. Vui lòng thử lại: " + e.getMessage());
        }
    }
}
