package com.fnb.menu.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.menu.dto.event.MenuUpdatedEvent;
import com.fnb.menu.entity.MenuItem;
import com.fnb.menu.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Lắng nghe các sự kiện từ inventory-service để đồng bộ trạng thái món ăn.
 * - menu.item_out_of_stock: Ẩn món khỏi QR menu khi hết nguyên liệu
 * - menu.item_in_stock:     Hiện lại món khi nguyên liệu có trở lại
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventConsumerService {

    private final MenuItemRepository menuItemRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "menu.item_out_of_stock", groupId = "menu-inventory-group")
    @Transactional
    @CacheEvict(value = {"menu:items", "menu:item"}, allEntries = true)
    public void handleItemOutOfStock(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            UUID menuItemId = UUID.fromString(payload.get("menuItemId").asText());
            String reason = payload.has("reason") ? payload.get("reason").asText() : "Hết nguyên liệu";

            Optional<MenuItem> itemOpt = menuItemRepository.findById(menuItemId);
            if (itemOpt.isEmpty()) {
                log.warn("[Inventory→Menu] Không tìm thấy MenuItem ID: {}, bỏ qua.", menuItemId);
                return;
            }

            MenuItem item = itemOpt.get();
            if (!item.isAvailable()) {
                log.info("[Inventory→Menu] MenuItem {} đã ở trạng thái không khả dụng, bỏ qua.", menuItemId);
                return;
            }

            item.setAvailable(false);
            menuItemRepository.save(item);

            log.warn("[Inventory→Menu] Ẩn món '{}' (ID: {}) - Lý do: {}", item.getName(), menuItemId, reason);

            // Bắn WebSocket cho tất cả client QR đang xem menu biết ngay lập tức
            eventPublisher.publishEvent(MenuUpdatedEvent.builder()
                    .itemId(item.getId())
                    .type("ITEM")
                    .isAvailable(false)
                    .isActive(item.isActive())
                    .updatedAt(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("[Inventory→Menu] Lỗi xử lý sự kiện menu.item_out_of_stock: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "menu.item_in_stock", groupId = "menu-inventory-group")
    @Transactional
    @CacheEvict(value = {"menu:items", "menu:item"}, allEntries = true)
    public void handleItemInStock(String message) {
        try {
            JsonNode payload = objectMapper.readTree(message);
            UUID menuItemId = UUID.fromString(payload.get("menuItemId").asText());

            Optional<MenuItem> itemOpt = menuItemRepository.findById(menuItemId);
            if (itemOpt.isEmpty()) {
                log.warn("[Inventory→Menu] Không tìm thấy MenuItem ID: {}, bỏ qua.", menuItemId);
                return;
            }

            MenuItem item = itemOpt.get();
            if (!item.isActive()) {
                log.info("[Inventory→Menu] MenuItem {} đã bị vô hiệu hóa bởi Admin, không tự động mở lại.", menuItemId);
                return;
            }

            if (item.isAvailable()) {
                log.info("[Inventory→Menu] MenuItem {} đã khả dụng rồi, bỏ qua.", menuItemId);
                return;
            }

            item.setAvailable(true);
            menuItemRepository.save(item);

            log.info("[Inventory→Menu] Mở lại món '{}' (ID: {}) - Nguyên liệu có hàng trở lại.", item.getName(), menuItemId);

            eventPublisher.publishEvent(MenuUpdatedEvent.builder()
                    .itemId(item.getId())
                    .type("ITEM")
                    .isAvailable(true)
                    .isActive(item.isActive())
                    .updatedAt(LocalDateTime.now())
                    .build());

        } catch (Exception e) {
            log.error("[Inventory→Menu] Lỗi xử lý sự kiện menu.item_in_stock: {}", e.getMessage(), e);
        }
    }
}
