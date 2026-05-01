package com.fnb.order.service;

import com.fnb.order.dto.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderCreatedEvent(OrderCreatedEvent event) {
        log.info("Gửi sự kiện ORDER_CREATED cho Ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("order.created", event.getOrderId().toString(), event);

        // Sau khi đặt hàng thành công (cart đã bị xóa ở backend),
        // bắn thêm event cart.updated để các client khác (cùng bàn) biết mà xóa giỏ
        // hàng local
        if (event.getSessionToken() != null) {
            log.info("Gửi sự kiện CART_UPDATED (xóa giỏ) cho session: {}", event.getSessionToken());
            CartUpdatedEvent cartEvent = CartUpdatedEvent.builder()
                    .sessionToken(event.getSessionToken())
                    .tableNumber(event.getTableNumber())
                    .build();
            kafkaTemplate.send("cart.updated", event.getSessionToken(), cartEvent);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStaffCallEvent(StaffCallCreatedEvent event) {
        log.info("Gửi sự kiện STAFF_CALL cho Table Number: {}", event.getTableNumber());
        kafkaTemplate.send("staff.called", event.getCallId().toString(), event);
    }

    @EventListener
    public void handleCartUpdatedEvent(CartUpdatedEvent event) {
        log.info("Gửi sự kiện CART_UPDATED cho session: {}", event.getSessionToken());
        kafkaTemplate.send("cart.updated", event.getSessionToken(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderPaidEvent(OrderPaidEvent event) {
        log.info("Gửi sự kiện ORDER_PAID cho Order ID: {}", event.getOrderId());
        kafkaTemplate.send("order.paid", event.getOrderId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketUpdatedEvent(TicketUpdatedEvent event) {
        log.info("Gửi sự kiện TICKET_UPDATED cho Ticket ID: {}", event.getTicketId());
        kafkaTemplate.send("ticket.updated", event.getTicketId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTableStatusUpdatedEvent(TableStatusUpdatedEvent event) {
        log.info("Gửi sự kiện TABLE_STATUS_UPDATED cho Table ID: {}", event.getTableId());
        kafkaTemplate.send("table.status_updated", event.getTableId().toString(), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusUpdatedEvent(OrderStatusUpdatedEvent event) {
        log.info("Gửi sự kiện ORDER_STATUS_UPDATED cho Order ID: {}", event.getOrderId());
        kafkaTemplate.send("order.status_updated", event.getOrderId().toString(), event);
    }
}
