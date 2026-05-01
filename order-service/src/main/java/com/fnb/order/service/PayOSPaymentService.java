package com.fnb.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.Webhook;
import vn.payos.model.webhooks.WebhookData;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSPaymentService {

    private final PayOS payOS;
    private final com.fnb.order.repository.OrderRepository orderRepository;
    private final OrderService orderService;
    private final org.springframework.context.ApplicationEventPublisher applicationEventPublisher;

    @Value("${app.qr-base-url}")
    private String qrBaseUrl;

    @Value("${payos.client-id}")
    private String clientId;

    /**
     * Creates a payment link or QR code through PayOS.
     */
    public String createPaymentLink(UUID orderId, String sessionToken, BigDecimal totalAmount, BigDecimal cashAmount) {
        try {
            Long orderCode = System.currentTimeMillis() / 1000; // Unique orderCode for PayOS

            // 1. Lấy đơn hàng và kiểm tra tính hợp lệ
            com.fnb.order.entity.Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Mã đơn hàng không hợp lệ"));
            
            // 2. Bảo mật: Xác minh xem đơn hàng này có thuộc về SessionToken gửi lên không
            if (sessionToken == null || order.getSession() == null || !sessionToken.equals(order.getSession().getSessionToken())) {
                log.warn("🚨 CẢNH BÁO BẢO MẬT: Có người cố tình tạo thanh toán cho đơn {} nhưng sai SessionToken!", orderId);
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện thanh toán cho đơn hàng này!");
            }

            // 3. Chuẩn bị yêu cầu gửi sang PayOS
            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(totalAmount.longValue())
                .description("TT Don hang " + orderCode)
                .returnUrl(qrBaseUrl.replace("?qr=", "") + "tracking?t=" + sessionToken + "&payment=success") 
                .cancelUrl(qrBaseUrl.replace("?qr=", "") + "payment?t=" + sessionToken) 
                .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            
            // 4. Lưu mapping mã PayOS để đối soát Webhook sau này
            order.setPayosOrderCode(orderCode);
            
            if (cashAmount != null && cashAmount.compareTo(BigDecimal.ZERO) > 0) {
                order.setPaymentMethod("MIXED");
                order.setPaymentDetail(String.format("{\"cash\": %s, \"qr\": %s}", cashAmount, totalAmount));
            } else {
                order.setPaymentMethod("PayOS");
            }
            
            orderRepository.save(order);

            log.info("Tạo link thanh toán PayOS thành công cho Order {}, PayOS Code: {}", orderId, orderCode);
            return response.getCheckoutUrl();

        } catch (org.springframework.security.access.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi tạo payment link với PayOS: ", e);
            throw new RuntimeException("Không thể tạo phiên thanh toán PayOS", e);
        }
    }

    /**
     * Lắng nghe biến động số dư từ Ngân hàng do PayOS báo về (Webhook)
     */
    public void handleWebhook(Webhook webhook) {
        try {
            // Gọi thư viện xác thực chữ ký
            WebhookData data = payOS.webhooks().verify(webhook);
            Long payosOrderCode = data.getOrderCode();
            log.info("✅ Xác thực Webhook PayOS thành công: Mã: {}, Tiền: {}", payosOrderCode, data.getAmount());

            // 1. Tìm Order trong Database theo payos_order_code
            com.fnb.order.entity.Order order = orderRepository.findByPayosOrderCode(payosOrderCode)
                .orElseThrow(() -> new Exception("Không tìm thấy đơn hàng tương ứng với mã " + payosOrderCode));
            
            // 2. Xác định số tiền cần thiết từ PayOS (Nếu là MIXED thì chỉ cần đủ phần qr)
            double requiredAmount = order.getTotal().doubleValue();
            boolean isMixed = "MIXED".equals(order.getPaymentMethod());

            if (isMixed && order.getPaymentDetail() != null) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(order.getPaymentDetail());
                    if (node.has("qr")) {
                        requiredAmount = node.get("qr").asDouble();
                    }
                } catch (Exception e) {
                    log.error("Lỗi đọc paymentDetail trong webhook", e);
                }
            }

            // 3. Nếu đã trả tiền đủ phần QR (hoặc toàn bộ nếu ko MIXED), tiến hành đóng đơn
            if (data.getAmount() >= requiredAmount) {
                log.info("🤑 Khách đã chuyển đủ phần QR ({}) cho đơn {}. Tiến hành auto-checkout.", data.getAmount(), order.getId());
                
                String finalMethod = isMixed ? "MIXED" : "PayOS";
                String finalDetail = order.getPaymentDetail(); 
                if (!isMixed) {
                    finalDetail = String.format("{\"bank_ref\": \"%s\", \"pay_amount\": %s}", data.getReference(), data.getAmount());
                }
                
                // Mượn logic closeOrder: Cập nhật PAID và bắn thông báo Real-time cho khách & quầy
                orderService.closeOrder(order.getId(), true, null, finalMethod, finalDetail); 
            } else {
                log.warn("❌ Khách bàn/mang về {} thanh toán thiếu tiền: Cần chuyển {}, nhưng chỉ gửi {}", order.getTable() != null ? order.getTable().getNumber() : "Mang về", requiredAmount, data.getAmount());
                
                // Bắn thông báo StaffCall cho Thu ngân đến xử lý tại bàn
                com.fnb.order.dto.event.StaffCallCreatedEvent event = com.fnb.order.dto.event.StaffCallCreatedEvent.builder()
                        .callId(java.util.UUID.randomUUID())
                        .sessionId(order.getSession().getId())
                        .tableId(order.getTable() != null ? order.getTable().getId() : null)
                        .tableNumber(order.getTable() != null ? order.getTable().getNumber() : null)
                        .callType("INSUFFICIENT_PAYMENT") 
                        .calledAt(java.time.LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }

        } catch(Exception e) {
             log.error("❌ PayOS Webhook Error: ", e);
             throw new RuntimeException("Xử lý Webhook thất bại", e);
        }
    }

    /**
     * Đồng bộ trạng thái thanh toán thủ công/theo lịch cho các đơn đang treo PAYMENT_REQUESTED.
     */
    public void syncPendingPayment(com.fnb.order.entity.Order order) {
        if (order.getPayosOrderCode() == null) return;

        try {
            // Lấy thông tin mới nhất từ PayOS
            var data = payOS.paymentRequests().get(order.getPayosOrderCode());
            String status = data.getStatus().name(); 
            
            if ("PAID".equals(status)) {
                log.info("💰 PayOS Sync Job: Đơn {} đã được thanh toán (status=PAID)! Tiến hành đóng đơn.", order.getId());
                orderService.closeOrder(order.getId(), true, null, "PayOS", null);
            } else if ("CANCELLED".equals(status)) {
                log.info("🚫 PayOS Sync Job: Link thanh toán đơn {} đã bị huỷ. Trả order về trạng thái OPEN.", order.getId());
                order.setStatus("OPEN");
                orderRepository.save(order);
                // Bắn event websocket nếu cần
            }
        } catch (Exception e) {
            log.error("PayOS Sync Job Error: Cập nhật lỗi cho đơn hàng {}", order.getId(), e);
        }
    }
}
