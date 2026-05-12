package com.fnb.order.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.order.entity.Order;
import com.fnb.order.entity.OrderTicket;
import com.fnb.order.entity.StaffCall;
import com.fnb.order.repository.OrderTicketRepository;
import com.fnb.order.repository.StaffCallRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketUpdateListener {

    private final ObjectMapper objectMapper;
    private final OrderTicketRepository ticketRepository;
    private final StaffCallRepository staffCallRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher applicationEventPublisher;

    private static final Map<UUID, Long> recentAlerts = new java.util.concurrent.ConcurrentHashMap<>();

    @KafkaListener(topics = "ticket.updated", groupId = "${spring.kafka.consumer.group-id:order-group}-immediate-alert")
    @Transactional
    public void consumeTicketUpdated(String payload) {
        try {
            JsonNode node = objectMapper.readTree(payload);
            
            if (!node.has("status") || (!"DONE".equals(node.get("status").asText()) && !"COMPLETED".equals(node.get("status").asText()))) {
                return;
            }

            String type = node.has("type") ? node.get("type").asText() : "";
            UUID ticketId = node.has("ticketId") && !node.get("ticketId").isNull() ? UUID.fromString(node.get("ticketId").asText()) : null;
            
            if (ticketId == null) return;

            OrderTicket ticket = ticketRepository.findById(ticketId).orElse(null);
            if (ticket == null || ticket.getOrder() == null) return;

            Order order = ticket.getOrder();
            boolean isTakeawayOrDelivery = "TAKEAWAY".equals(order.getOrderType()) || "DELIVERY".equals(order.getOrderType());

            if (isTakeawayOrDelivery) {
                // Takeaway Rule: Check if 100% DONE
                if ("TICKET".equals(type) || "ITEM".equals(type)) {
                    boolean hasPending = order.getTickets().stream()
                        .flatMap(t -> t.getItems().stream())
                        .anyMatch(i -> "PENDING".equals(i.getStatus()) || "PREPARING".equals(i.getStatus()));
                    
                    if (!hasPending) {
                        log.info("Takeaway Order {} is 100% DONE. Creating StaffCall.", order.getId());
                        
                        if (order.getSession() != null) {
                            boolean alertExists = staffCallRepository.existsBySessionIdAndCallTypeAndStatus(order.getSession().getId(), "TAKEAWAY_READY", "PENDING");
                            if (!alertExists) {
                                String shortId = order.getId().toString().substring(0,4).toUpperCase();
                                StaffCall call = StaffCall.builder()
                                        .session(order.getSession())
                                        .table(null) // Takeaway có thể không có bàn
                                        .callType("TAKEAWAY_READY")
                                        .status("PENDING")
                                        .message("Đơn Mang đi #" + shortId + " đã xong - Mời trả khách")
                                        .build();
                                staffCallRepository.save(call);

                                // Publish event so OrderProducerService can send via Kafka
                                com.fnb.order.dto.event.StaffCallCreatedEvent event = com.fnb.order.dto.event.StaffCallCreatedEvent.builder()
                                        .callId(call.getId())
                                        .sessionId(order.getSession().getId())
                                        .tableId(null)
                                        .tableNumber(null)
                                        .callType("TAKEAWAY_READY")
                                        .calledAt(LocalDateTime.now())
                                        .build();
                                applicationEventPublisher.publishEvent(event);

                                // Bắn ngay sang POS để kêu
                                Map<String, Object> alertEvent = new HashMap<>();
                                alertEvent.put("tableNumber", null);
                                alertEvent.put("zone", "Takeaway");
                                alertEvent.put("message", "Đơn Mang đi #" + shortId + " đã xong - Mời trả khách");
                                alertEvent.put("urgencyLevel", "TAKEAWAY_READY");
                                alertEvent.put("alertAt", LocalDateTime.now().toString());

                                kafkaTemplate.send("delivery.ready.alert", order.getId().toString(), alertEvent);
                            }
                        }
                    }
                }
            } else {
                // Dine-in Rule: Alert immediately when an item or ticket is DONE
                if ("ITEM".equals(type) || "TICKET".equals(type)) {
                    long now = System.currentTimeMillis();
                    Long lastAlert = recentAlerts.get(ticketId);
                    
                    // Debounce 2 giây cho cùng một ticket để tránh nổ chuông kép khi hoàn thành món cuối (kích hoạt cả ITEM và TICKET event)
                    if (lastAlert != null && (now - lastAlert) < 2000) {
                        return;
                    }
                    recentAlerts.put(ticketId, now);
                    
                    // Dọn dẹp memory
                    if (recentAlerts.size() > 1000) {
                        recentAlerts.entrySet().removeIf(e -> (now - e.getValue()) > 10000);
                    }

                    Integer tableNumber = order.getTable() != null ? order.getTable().getNumber() : null;
                    
                    Map<String, Object> alertEvent = new HashMap<>();
                    alertEvent.put("tableNumber", tableNumber);
                    alertEvent.put("zone", order.getTable() != null ? order.getTable().getZone() : "Dine-in");
                    alertEvent.put("message", "Bàn " + tableNumber + " có món mới xong!");
                    alertEvent.put("urgencyLevel", "NEW_ITEM");
                    alertEvent.put("alertAt", LocalDateTime.now().toString());

                    kafkaTemplate.send("delivery.ready.alert", order.getId().toString(), alertEvent);
                }
            }

        } catch (Exception e) {
            log.error("Error processing immediate alert for ticket.updated", e);
        }
    }
}
