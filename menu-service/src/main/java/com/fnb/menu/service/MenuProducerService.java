package com.fnb.menu.service;

import com.fnb.menu.dto.event.MenuUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

@Service
@RequiredArgsConstructor
@Slf4j
public class MenuProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleMenuUpdatedEvent(MenuUpdatedEvent event) {
        try {
            log.info("Gửi sự kiện MENU_UPDATED cho {}: {}, Trạng thái khả dụng: {}", 
                event.getType(), 
                "ITEM".equals(event.getType()) ? event.getItemId() : event.getOptionId(),
                event.isAvailable());
            
            kafkaTemplate.send("menu.updated", 
                event.getItemId() != null ? event.getItemId().toString() : "GLOBAL", 
                event);
        } catch (Exception e) {
            log.error("Kafka send error: {}", e.getMessage());
        }
    }
}
