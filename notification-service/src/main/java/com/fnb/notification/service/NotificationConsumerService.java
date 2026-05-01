package com.fnb.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumerService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order.created", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderCreated(String payload) {
        log.info("Received order.created: {}", payload);
        // Bắn cho KDS biết có đơn mới
        messagingTemplate.convertAndSend("/topic/kds/orders", payload);
        
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("sessionToken")) {
                String sessionToken = node.get("sessionToken").asText();
                messagingTemplate.convertAndSend("/topic/sessions/" + sessionToken + "/orders", payload);
            }
            // Bắn lên topic tổng cho POS cập nhật sơ đồ bàn (luôn bắn)
            messagingTemplate.convertAndSend("/topic/pos/tables", payload);
        } catch (Exception e) {
            log.error("Error parsing order.created payload", e);
        }
    }

    @KafkaListener(topics = "staff.called", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeStaffCalled(String payload) {
        log.info("Received staff.called: {}", payload);
        // Bắn cho mành hình của thu ngân / nhân viên phục vụ
        messagingTemplate.convertAndSend("/topic/staff/calls", payload);
        // Bắn luôn cho Table Map để cập nhật trạng thái nếu là gọi thanh toán
        messagingTemplate.convertAndSend("/topic/pos/tables", payload);
    }

    @KafkaListener(topics = "cart.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCartUpdated(String payload) {
        log.info("Received cart.updated: {}", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("sessionToken")) {
                String sessionToken = node.get("sessionToken").asText();
                // Broadcast để những ai chung session (chung bàn) biết giỏ hàng thay đổi
                messagingTemplate.convertAndSend("/topic/sessions/" + sessionToken + "/cart", payload);
            }
        } catch (Exception e) {
            log.error("Error parsing cart.updated payload", e);
        }
    }

    @KafkaListener(topics = "order.paid", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderPaid(String payload) {
        log.info("Received order.paid: {}", payload);
        // Báo cho KDS hoặc khách là bàn đã thanh toán
        messagingTemplate.convertAndSend("/topic/orders/paid", payload);
        
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("sessionToken") && !node.get("sessionToken").isNull()) {
                String sessionToken = node.get("sessionToken").asText();
                messagingTemplate.convertAndSend("/topic/sessions/" + sessionToken + "/paid", payload);
            }
            // Bắn lên topic tổng cho POS cập nhật sơ đồ bàn (trạng thái bàn -> FREE)
            messagingTemplate.convertAndSend("/topic/pos/tables", payload);
        } catch (Exception e) {
            log.error("Error parsing order.paid payload", e);
        }
    }

    @KafkaListener(topics = "ticket.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTicketUpdated(String payload) {
        log.info("Received ticket.updated: {}", payload);
        // Luôn bắn lên KDS để màn hình bếp nhận được mọi thay đổi
        messagingTemplate.convertAndSend("/topic/kds/tickets", payload);
        // Bắn lên cho POS để cập nhật sơ đồ bàn (vì hủy món có thể đổi tổng giá hoặc trạng thái bàn)
        messagingTemplate.convertAndSend("/topic/pos/tables", payload);
        // Bắn lên topic chung cho các màn hình POS Detail lắng nghe (không phụ thuộc session)
        messagingTemplate.convertAndSend("/topic/pos/updates", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            
            // Lọc riêng cho Server: Chỉ bắn vào topic khay đồ nếu trạng thái là DONE, COMPLETED hoặc SERVED
            if (node.has("status")) {
                String status = node.get("status").asText();
                if ("DONE".equals(status) || "COMPLETED".equals(status) || "SERVED".equals(status)) {
                    messagingTemplate.convertAndSend("/topic/server/deliveries", payload);
                }
            }

            if (node.has("sessionToken") && !node.get("sessionToken").isNull()) {
                String sessionToken = node.get("sessionToken").asText();
                // Bắn cho màn hình khách tracking để biết món đang làm hay đã xong
                messagingTemplate.convertAndSend("/topic/sessions/" + sessionToken + "/tickets", payload);
            }
        } catch (Exception e) {
            log.error("Error parsing ticket.updated payload", e);
        }
    }

    @KafkaListener(topics = "order.status_updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeOrderStatusUpdated(String payload) {
        log.info("Received order.status_updated: {}", payload);
        try {
            JsonNode node = objectMapper.readTree(payload);
            if (node.has("sessionToken")) {
                String sessionToken = node.get("sessionToken").asText();
                messagingTemplate.convertAndSend("/topic/sessions/" + sessionToken + "/orders", payload);
            }
            // Bắn lên topic tổng cho POS cập nhật sơ đồ bàn (luôn bắn khi đổi trạng thái đơn)
            messagingTemplate.convertAndSend("/topic/pos/tables", payload);
        } catch (Exception e) {
            log.error("Error parsing order.status_updated payload", e);
        }
    }

    @KafkaListener(topics = "table.status_updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeTableStatusUpdated(String payload) {
        log.info("Received table.status_updated: {}", payload);
        // Bắn lên topic tổng cho POS cập nhật sơ đồ bàn ngay lập tức (khi mở bàn/đóng bàn)
        messagingTemplate.convertAndSend("/topic/pos/tables", payload);
    }

    @KafkaListener(topics = "menu.updated", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMenuUpdated(String payload) {
        log.info("Received menu.updated: {}", payload);
        // Broadcast tới tất cả mọi người để cập nhật Menu (ẩn/hiện món hết hàng)
        messagingTemplate.convertAndSend("/topic/menu/updates", payload);
    }

    // ===================================================================
    // Server Role Topics (Phase 3)
    // ===================================================================

    /**
     * Sweeper cảnh báo món trễ: push xuống cả channel chung và channel theo zone.
     * FE Server subscribe /topic/server/alerts và /topic/server/zone/{zone}.
     */
    @KafkaListener(topics = "delivery.ready.alert", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeDeliveryReadyAlert(String payload) {
        log.warn("Received delivery.ready.alert: {}", payload);
        // Broadcast tới tất cả Server
        messagingTemplate.convertAndSend("/topic/server/alerts", payload);
        // Cũng push xuống zone-specific channel nếu có
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
            if (node.has("zone") && !node.get("zone").isNull()) {
                String zone = node.get("zone").asText();
                messagingTemplate.convertAndSend("/topic/server/zone/" + zone, payload);
            }
        } catch (Exception e) {
            log.error("Error parsing delivery.ready.alert payload", e);
        }
    }

    /**
     * Server A tiếp nhận yêu cầu: push xuống tất cả Server để lock UI.
     * FE nhận event này sẽ disable nút "Tiếp nhận" của các Server khác.
     */
    @KafkaListener(topics = "staff.call.accepted", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeStaffCallAccepted(String payload) {
        log.info("Received staff.call.accepted: {}", payload);
        // Push xuống tất cả Server đang online
        messagingTemplate.convertAndSend("/topic/server/alerts", payload);
        // Push xuống cả POS để hiển thị ai đang xử lý
        messagingTemplate.convertAndSend("/topic/staff/calls", payload);
    }

    /**
     * Server hoàn thành yêu cầu: push xuống để ẩn khỏi UI.
     */
    @KafkaListener(topics = "staff.call.resolved", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeStaffCallResolved(String payload) {
        log.info("Received staff.call.resolved: {}", payload);
        // Push xuống cả POS và Server để clear yêu cầu này khỏi màn hình
        messagingTemplate.convertAndSend("/topic/staff/calls", payload);
    }

    /**
     * Sweeper Spillover: yêu cầu chưa được tiếp nhận quá 30s.
     * Broadcast cảnh báo đỏ đến Tất Cả Server (không chỉ zone gốc).
     */
    @KafkaListener(topics = "staff.call.spillover", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeStaffCallSpillover(String payload) {
        log.warn("Received staff.call.spillover: {}", payload);
        // Cướng độ: push xuống tất cả Server ở mọi zone
        messagingTemplate.convertAndSend("/topic/server/alerts", payload);
    }

    /**
     * Cancel Alert: Thu ngân hủy món khi bếp ĐÃ LÀM XONG nhưng Server CHƯA BƯNG.
     * Push cảnh báo ĐỎ KHẨN CẤP xuống tất cả Server để tránh bưng nhầm.
     * Frontend sẽ trigger Haptic + Audio ping khi nhận event này.
     */
    @KafkaListener(topics = "cancel.alert", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeCancelAlert(String payload) {
        log.warn("Received cancel.alert: {}", payload);
        // Push tới tất cả Server App đang online
        messagingTemplate.convertAndSend("/topic/server/alerts", payload);
        // Cũng push vào channel zone nếu biết
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
            if (node.has("tableNumber") && !node.get("tableNumber").isNull()) {
                // Push vào kênh chung để POS biết có hủy khẩn
                messagingTemplate.convertAndSend("/topic/pos/updates", payload);
            }
        } catch (Exception e) {
            log.error("Error parsing cancel.alert payload", e);
        }
    }
}
