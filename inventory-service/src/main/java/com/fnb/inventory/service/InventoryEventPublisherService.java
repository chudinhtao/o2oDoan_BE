package com.fnb.inventory.service;

import com.fnb.inventory.dto.event.InventoryOutOfStockEvent;
import com.fnb.inventory.dto.event.InventoryInStockEvent;
import com.fnb.inventory.entity.Recipe;
import com.fnb.inventory.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fnb.inventory.enums.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryEventPublisherService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RecipeRepository recipeRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryOutOfStock(InventoryOutOfStockEvent event) {
        log.info("Xử lý sự kiện hết hàng cho nguyên liệu: {}", event.getInventoryItemName());

        // Query có điều kiện thay vì findAll() toàn bộ bảng
        List<Recipe> affectedRecipes = recipeRepository.findByInventoryItemId(event.getInventoryItemId());

        for (Recipe recipe : affectedRecipes) {
            UUID saleItemId = recipe.getSaleItemId();
            log.warn("Món (saleItemId={}) sẽ bị ẩn vì thiếu nguyên liệu {}", saleItemId, event.getInventoryItemName());

            Map<String, Object> outOfStockEvent = new HashMap<>();
            outOfStockEvent.put("menuItemId", saleItemId);
            outOfStockEvent.put("reason", "Hết nguyên liệu: " + event.getInventoryItemName());

            kafkaTemplate.send("menu.item_out_of_stock", saleItemId.toString(), outOfStockEvent);
        }
    }

    /**
     * Khi nguyên liệu được nhập thêm (IN_PO hoặc IN_QUICK) và vượt mức 0,
     * publish event để menu-service tự động mở lại các món liên quan.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleInventoryInStock(InventoryInStockEvent event) {
        log.info("Nguyên liệu '{}' có hàng trở lại, kiểm tra các món cần mở lại.", event.getInventoryItemName());

        List<Recipe> affectedRecipes = recipeRepository.findByInventoryItemId(event.getInventoryItemId());

        for (Recipe recipe : affectedRecipes) {
            UUID saleItemId = recipe.getSaleItemId();
            log.info("Mở lại món (saleItemId={}) vì nguyên liệu {} có hàng trở lại.", saleItemId, event.getInventoryItemName());

            Map<String, Object> inStockEvent = new HashMap<>();
            inStockEvent.put("menuItemId", saleItemId);
            inStockEvent.put("reason", "Nguyên liệu có hàng trở lại: " + event.getInventoryItemName());

            kafkaTemplate.send("menu.item_in_stock", saleItemId.toString(), inStockEvent);
        }
    }
}
