package com.fnb.menu.service;

import com.fnb.menu.dto.request.PromotionRequest;
import com.fnb.menu.dto.response.PromotionResponse;
import com.fnb.menu.entity.*;
import com.fnb.menu.repository.PromotionRepository;
import com.fnb.common.dto.PageResponse;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final com.fnb.menu.repository.MenuItemRepository menuItemRepository;
    private final com.fnb.menu.repository.CategoryRepository categoryRepository;

    // =========================================================
    // ADMIN: CRUD
    // =========================================================

    public PageResponse<PromotionResponse> listForAdmin(String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? "" : keyword;
        Page<Promotion> result = promotionRepository.findAllForAdmin(
                kw, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional
    public PromotionResponse create(PromotionRequest request) {
        validateRequest(request);

        Promotion promo = Promotion.builder()
                .code(request.getCode() != null && !request.getCode().isBlank() 
                        ? request.getCode().trim().toUpperCase() 
                        : null)
                .name(request.getName())
                .scope(request.getScope())
                .triggerType(request.getTriggerType())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .maxDiscount(request.getMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .priority(request.getPriority())
                .startAt(request.getStartAt())
                .endAt(request.getEndAt())
                .build();
        promo.setStackable(request.isStackable());

        promotionRepository.save(promo); // cần save trước để có ID cho relations

        applyRelations(promo, request);
        return toResponse(promotionRepository.save(promo));
    }

    @Transactional
    public PromotionResponse update(UUID id, PromotionRequest request) {
        validateRequest(request);
        Promotion promo = findById(id);

        promo.setName(request.getName());
        promo.setCode(request.getCode() != null && !request.getCode().isBlank() 
                ? request.getCode().trim().toUpperCase() 
                : null);
        promo.setScope(request.getScope());
        promo.setTriggerType(request.getTriggerType());
        promo.setDiscountType(request.getDiscountType());
        promo.setDiscountValue(request.getDiscountValue());
        promo.setMaxDiscount(request.getMaxDiscount());
        promo.setUsageLimit(request.getUsageLimit());
        promo.setPriority(request.getPriority());
        promo.setStartAt(request.getStartAt());
        promo.setEndAt(request.getEndAt());
        promo.setStackable(request.isStackable());
        promo.setUpdatedAt(LocalDateTime.now());

        // Clear & rebuild relations
        promo.getTargets().clear();
        promo.getEntityBundleItems().clear();
        promo.getRequirements().clear();
        promo.getSchedules().clear();

        applyRelations(promo, request);
        return toResponse(promotionRepository.save(promo));
    }

    @Transactional
    public void delete(UUID id) {
        Promotion promo = findById(id);
        promo.setActive(false);
        promo.setUpdatedAt(LocalDateTime.now());
        promotionRepository.save(promo);
        log.info("Đã tạm dừng CTKM '{}'", promo.getName());
    }

    @Transactional
    public void hardDelete(UUID id) {
        promotionRepository.delete(findById(id));
    }

    @Transactional
    public PromotionResponse toggleStatus(UUID id) {
        Promotion promo = findById(id);
        promo.setActive(!promo.isActive());
        promo.setUpdatedAt(LocalDateTime.now());
        return toResponse(promotionRepository.save(promo));
    }

    // =========================================================
    // PUBLIC: Validate Coupon (Pricing Engine Level 3)
    // =========================================================

    /**
     * Validate mã Coupon — dùng ở bước khách nhập mã trên Order.
     * Không tính discount ở đây, chỉ trả về thông tin để OrderService tính.
     */
    public PromotionResponse validateCoupon(String code, BigDecimal subtotal) {
        Promotion promo = promotionRepository.findByCodeAndActiveTrue(code)
                .orElseThrow(() -> new BusinessException("Mã giảm giá không hợp lệ hoặc đã hết hạn"));

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartAt() != null && now.isBefore(promo.getStartAt())) {
            throw new BusinessException("Mã chưa có hiệu lực");
        }
        if (promo.getEndAt() != null && now.isAfter(promo.getEndAt())) {
            throw new BusinessException("Mã đã hết hạn");
        }
        if (!isWithinSchedule(promo, now)) {
            throw new BusinessException("Mã không áp dụng trong khung giờ này");
        }
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new BusinessException("Mã giảm giá đã hết lượt sử dụng");
        }

        // Check điều kiện đơn tối thiểu nếu có Requirement
        promo.getRequirements().forEach(req -> {
            if (subtotal != null && subtotal.compareTo(req.getMinOrderAmount()) < 0) {
                throw new BusinessException(
                        "Đơn hàng tối thiểu " + req.getMinOrderAmount() + "đ để áp mã này");
            }
        });

        return toResponse(promo);
    }

    /**
     * Lấy danh sách toàn bộ CTKM đang active — Pricing Engine của OrderService dùng.
     */
    public List<PromotionResponse> getActivePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findAllActive(now)
                .stream()
                .filter(p -> isWithinSchedule(p, now))
                .map(this::toResponse).toList();
    }

    /**
     * Lấy CTKM active theo scope.
     */
    public List<PromotionResponse> getActiveByScope(String scope) {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActiveByScope(scope, now)
                .stream()
                .filter(p -> isWithinSchedule(p, now))
                .map(this::toResponse).toList();
    }

    @Transactional
    public void incrementUsedCount(UUID id) {
        Promotion promo = findById(id);
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            log.warn("Mã {} đã đạt giới hạn sử dụng.", promo.getCode());
            return;
        }
        promo.setUsedCount(promo.getUsedCount() + 1);
        promotionRepository.save(promo);
    }

    // =========================================================
    // Private helpers
    // =========================================================

    private void validateRequest(PromotionRequest r) {
        if ("COUPON".equals(r.getTriggerType()) && (r.getCode() == null || r.getCode().isBlank())) {
            throw new BusinessException("Mã Coupon là bắt buộc khi chọn hình thức kích hoạt bằng Coupon");
        }
        
        if ("PERCENT".equals(r.getDiscountType()) && r.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new BusinessException("Giá trị giảm theo phần trăm không được vượt quá 100%");
        }

        if (r.getStartAt() != null && r.getEndAt() != null && r.getStartAt().isAfter(r.getEndAt())) {
            throw new BusinessException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        if ("BUNDLE".equals(r.getScope()) && (r.getBundleItems() == null || r.getBundleItems().isEmpty())) {
            throw new BusinessException("Loại COMBO yêu cầu ít nhất 1 món ăn điều kiện");
        }

        // Validate schedules
        if (r.getSchedules() != null && !r.getSchedules().isEmpty()) {
            for (PromotionRequest.ScheduleRequest s : r.getSchedules()) {
                if (s.getStartTime() != null && s.getEndTime() != null && !s.getStartTime().isBefore(s.getEndTime())) {
                    throw new BusinessException("Trong lịch lặp lại, giờ bắt đầu phải trước giờ kết thúc (Thứ " + (s.getDayOfWeek() == 0 ? "CN" : s.getDayOfWeek() + 1) + ")");
                }
            }
        }
    }

    private void applyRelations(Promotion promo, PromotionRequest r) {
        // Targets
        if (r.getTargets() != null) {
            r.getTargets().forEach(t -> promo.getTargets().add(
                    PromotionTarget.builder()
                            .promotion(promo)
                            .targetType(t.getTargetType())
                            .targetId(t.getTargetId())
                            .build()));
        }

        // Bundle Items
        if (r.getBundleItems() != null) {
            r.getBundleItems().forEach(b -> promo.getEntityBundleItems().add(
                    PromotionBundleItem.builder()
                            .promotion(promo)
                            .itemId(b.getItemId())
                            .quantity(b.getQuantity())
                            .role(b.getRole())
                            .build()));
        }

        // Requirement
        if (r.getRequirement() != null) {
            PromotionRequest.RequirementRequest req = r.getRequirement();
            promo.getRequirements().add(
                    PromotionRequirement.builder()
                            .promotion(promo)
                            .minOrderAmount(req.getMinOrderAmount() != null
                                    ? req.getMinOrderAmount() : BigDecimal.ZERO)
                            .minQuantity(req.getMinQuantity())
                            .memberLevel(req.getMemberLevel())
                            .build());
        }

        // Schedules
        if (r.getSchedules() != null) {
            r.getSchedules().forEach(s -> promo.getSchedules().add(
                    PromotionSchedule.builder()
                            .promotion(promo)
                            .dayOfWeek(s.getDayOfWeek())
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .build()));
        }
    }

    /**
     * Kiểm tra CTKM có đang trong lịch khung giờ (Happy Hour) không.
     * Nếu không có Schedule thì always pass.
     */
    private boolean isWithinSchedule(Promotion promo, LocalDateTime now) {
        if (promo.getSchedules().isEmpty()) return true;
        int todayDow = now.getDayOfWeek().getValue() % 7; // Java: MON=1, Sun=7 -> map Sun=0
        return promo.getSchedules().stream().anyMatch(s ->
                s.getDayOfWeek() == todayDow
                        && !now.toLocalTime().isBefore(s.getStartTime())
                        && !now.toLocalTime().isAfter(s.getEndTime()));
    }

    private Promotion findById(UUID id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Khuyến mãi không tồn tại"));
    }

    private PromotionResponse toResponse(Promotion p) {
        LocalDateTime now = LocalDateTime.now();
        String status = "ACTIVE";
        
        if (!p.isActive()) {
            status = "DISABLED";
        } else if (p.getStartAt() != null && p.getStartAt().isAfter(now)) {
            status = "SCHEDULED";
        } else if (p.getEndAt() != null && p.getEndAt().isBefore(now)) {
            status = "EXPIRED";
        }

        return PromotionResponse.builder()
                .id(p.getId())
                .code(p.getCode())
                .name(p.getName())
                .scope(p.getScope())
                .triggerType(p.getTriggerType())
                .discountType(p.getDiscountType())
                .discountValue(p.getDiscountValue())
                .maxDiscount(p.getMaxDiscount())
                .usageLimit(p.getUsageLimit())
                .usedCount(p.getUsedCount())
                .priority(p.getPriority())
                .startAt(p.getStartAt())
                .endAt(p.getEndAt())
                .stackable(p.isStackable())
                .active(p.isActive())
                .createdAt(p.getCreatedAt())
                .displayStatus(status)
                .targets(p.getTargets().stream().map(t -> {
                    String tName = "Unknown";
                    if ("PRODUCT".equals(t.getTargetType())) {
                        tName = menuItemRepository.findById(t.getTargetId()).map(com.fnb.menu.entity.MenuItem::getName).orElse(tName);
                    } else if ("CATEGORY".equals(t.getTargetType())) {
                        tName = categoryRepository.findById(t.getTargetId()).map(com.fnb.menu.entity.Category::getName).orElse(tName);
                    }
                    return PromotionResponse.TargetResponse.builder()
                            .id(t.getId()).targetType(t.getTargetType()).targetId(t.getTargetId())
                            .targetName(tName)
                            .build();
                }).toList())
                .bundleItems(p.getEntityBundleItems().stream().map(b -> {
                    String bName = menuItemRepository.findById(b.getItemId()).map(com.fnb.menu.entity.MenuItem::getName).orElse("Unknown");
                    return PromotionResponse.BundleItemResponse.builder()
                            .id(b.getId()).itemId(b.getItemId())
                            .itemName(bName)
                            .quantity(b.getQuantity()).role(b.getRole())
                            .build();
                }).toList())
                .requirement(p.getRequirements().stream().findFirst().map(r ->
                        PromotionResponse.RequirementResponse.builder()
                                .id(r.getId()).minOrderAmount(r.getMinOrderAmount())
                                .minQuantity(r.getMinQuantity()).memberLevel(r.getMemberLevel())
                                .build()).orElse(null))
                .schedules(p.getSchedules().stream().map(s ->
                        PromotionResponse.ScheduleResponse.builder()
                                .id(s.getId()).dayOfWeek(s.getDayOfWeek())
                                .startTime(s.getStartTime()).endTime(s.getEndTime())
                                .build()).toList())
                .build();
    }
}
