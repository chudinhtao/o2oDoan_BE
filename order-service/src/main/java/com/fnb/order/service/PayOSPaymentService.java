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
import com.fnb.order.dto.event.StaffCallCreatedEvent;
import com.fnb.order.entity.Order;
import com.fnb.order.repository.OrderRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSPaymentService {

    private final PayOS payOS;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final com.fnb.order.repository.ReservationRepository reservationRepository;

    @Value("${app.qr-base-url}")
    private String qrBaseUrl;

    @Value("${payos.client-id}")
    private String clientId;

    /**
     * Creates a payment link or QR code through PayOS.
     */
    @Transactional
    public java.util.Map<String, String> createPaymentLink(UUID orderId, String sessionToken, BigDecimal totalAmount, BigDecimal cashAmount) {
        try {
            Long orderCode = System.currentTimeMillis() / 1000; // Unique orderCode for PayOS

            // 1. Lấy đơn hàng và kiểm tra tính hợp lệ
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Mã đơn hàng không hợp lệ"));
            
            // 2. Bảo mật: Xác minh xem đơn hàng này có thuộc về SessionToken gửi lên không
            if (sessionToken == null || order.getSession() == null || !sessionToken.equals(order.getSession().getSessionToken())) {
                log.warn("🚨 CẢNH BÁO BẢO MẬT: Có người cố tình tạo thanh toán cho đơn {} nhưng sai SessionToken!", orderId);
                throw new AccessDeniedException("Bạn không có quyền thực hiện thanh toán cho đơn hàng này!");
            }

            // 3. Xử lý trường hợp số tiền thanh toán = 0 (Ví dụ: áp mã giảm giá 100%)
            String baseUrl = qrBaseUrl.replace("/table?qr=", "/");
            if (!baseUrl.endsWith("/")) {
                baseUrl += "/";
            }

            if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
                log.info("Đơn hàng {} có số tiền thanh toán <= 0, tiến hành đóng đơn hàng tự động.", orderId);
                orderService.closeOrder(order.getId(), true, null, "VOUCHER", "{\"paid_by_voucher\": true}");
                
                java.util.Map<String, String> result = new java.util.HashMap<>();
                // Trả về trang thanh toán, khi load lại sẽ thấy trạng thái PAID
                result.put("checkoutUrl", baseUrl + "payment?t=" + sessionToken);
                return result;
            }

            // 4. Chuẩn bị yêu cầu gửi sang PayOS

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(totalAmount.longValue())
                .description("TT Don hang " + orderCode)
                .returnUrl(baseUrl + "tracking?t=" + sessionToken + "&payment=success") 
                .cancelUrl(baseUrl + "payment?t=" + sessionToken) 
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
            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("checkoutUrl", response.getCheckoutUrl());
            if (response.getQrCode() != null) {
                result.put("qrCode", response.getQrCode());
            }
            return result;

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Lỗi khi tạo payment link với PayOS: ", e);
            throw new com.fnb.common.exception.BusinessException("Không thể tạo phiên thanh toán PayOS: " + e.getMessage());
        }
    }

    /**
     * Creates a payment link or QR code through PayOS for a Reservation Deposit.
     */
    @Transactional
    public java.util.Map<String, String> createReservationDepositLink(UUID reservationId, String redirectUrl, Long amount) {
        try {
            Long orderCode = System.currentTimeMillis() / 1000; // Unique orderCode for PayOS

            com.fnb.order.entity.Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Mã đặt bàn không hợp lệ"));

            long depositAmount = (amount != null && amount > 0) ? amount : 200000L;

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                .orderCode(orderCode)
                .amount(depositAmount)
                .description("Coc dat ban " + orderCode)
                .returnUrl(redirectUrl + "?status=success") 
                .cancelUrl(redirectUrl + "?status=cancel") 
                .build();

            CreatePaymentLinkResponse response = payOS.paymentRequests().create(request);
            
            reservation.setPayosOrderCode(orderCode);
            reservationRepository.save(reservation);

            log.info("Tạo link cọc PayOS thành công cho Reservation {}, PayOS Code: {}", reservationId, orderCode);
            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("checkoutUrl", response.getCheckoutUrl());
            if (response.getQrCode() != null) {
                result.put("qrCode", response.getQrCode());
            }
            return result;

        } catch (Exception e) {
            log.error("Lỗi khi tạo payment link deposit với PayOS: ", e);
            throw new com.fnb.common.exception.BusinessException("Không thể tạo phiên đặt cọc PayOS: " + e.getMessage());
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
            Optional<Order> orderOpt = orderRepository.findByPayosOrderCode(payosOrderCode);
            
            if (orderOpt.isPresent()) {
                handleOrderWebhook(data, orderOpt.get());
            } else {
                // Thử tìm trong ReservationRepository
                Optional<com.fnb.order.entity.Reservation> resOpt = reservationRepository.findByPayosOrderCode(payosOrderCode);
                if (resOpt.isPresent()) {
                    handleReservationWebhook(data, resOpt.get());
                } else {
                    log.warn("Không tìm thấy Order hay Reservation nào khớp với mã {}", payosOrderCode);
                }
            }

        } catch(Exception e) {
             log.error("❌ PayOS Webhook Error: ", e);
             throw new com.fnb.common.exception.BusinessException("Xử lý Webhook thất bại: " + e.getMessage());
        }
    }

    private void handleReservationWebhook(WebhookData data, com.fnb.order.entity.Reservation reservation) {
        try {
            var payosData = payOS.paymentRequests().get(data.getOrderCode());
            long requestedAmount = payosData.getAmount();
            
            BigDecimal currentDeposit = reservation.getDepositAmount() != null ? reservation.getDepositAmount() : BigDecimal.ZERO;
            BigDecimal addedDeposit = BigDecimal.valueOf(data.getAmount());
            reservation.setDepositAmount(currentDeposit.add(addedDeposit));
            
            if (data.getAmount() >= requestedAmount) {
                log.info("🤑 Khách đã chuyển đủ cọc {} cho Reservation {}. Tiến hành auto-confirm.", data.getAmount(), reservation.getId());
                reservation.setStatus("CONFIRMED");
            } else {
                log.warn("❌ Khách chuyển THIẾU cọc cho Reservation {}. Nhận được {}, yêu cầu {}.", reservation.getId(), data.getAmount(), requestedAmount);
                // Giữ nguyên trạng thái PENDING và bắn thông báo
                StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                        .callId(UUID.randomUUID())
                        .sessionId(null) // Lễ tân sẽ nhận được thông báo chung
                        .tableId(null)
                        .tableNumber(null)
                        .callType("INSUFFICIENT_RESERVATION_DEPOSIT")
                        .calledAt(LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }
            reservationRepository.save(reservation);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý reservation webhook data", e);
        }
    }

    private void handleOrderWebhook(WebhookData data, Order order) {
            
            // 2. Xác định số tiền cần thiết từ PayOS (Nếu là MIXED thì chỉ cần đủ phần qr)
            double requiredAmount = order.getTotal().doubleValue();
            boolean isMixed = "MIXED".equals(order.getPaymentMethod());

            if (isMixed && order.getPaymentDetail() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(order.getPaymentDetail());
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
                StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                        .callId(UUID.randomUUID())
                        .sessionId(order.getSession().getId())
                        .tableId(order.getTable() != null ? order.getTable().getId() : null)
                        .tableNumber(order.getTable() != null ? order.getTable().getNumber() : null)
                        .callType("INSUFFICIENT_PAYMENT") 
                        .calledAt(LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }
    }

    /**
     * Đồng bộ trạng thái thanh toán thủ công/theo lịch cho các đơn đang treo PAYMENT_REQUESTED.
     */
    public void syncPendingPayment(Order order) {
        if (order.getPayosOrderCode() == null) return;

        try {
            // Lấy thông tin mới nhất từ PayOS
            var data = payOS.paymentRequests().get(order.getPayosOrderCode());
            String status = data.getStatus().name(); 
            
            if ("PAID".equals(status)) {
                log.info("💰 PayOS Sync Job: Đơn {} đã được thanh toán (status=PAID)! Tiến hành đóng đơn.", order.getId());
                orderService.closeOrder(order.getId(), true, null, "PayOS", null);
            } else if ("CANCELLED".equals(status)) {
                log.info("🚫 PayOS Sync Job: Link thanh toán QR PayOS của đơn {} đã hết hạn/bị huỷ. Bắn cảnh báo cho POS xử lý.", order.getId());
                
                // Bắn thông báo StaffCall cho Thu ngân thay vì tự lùi về OPEN
                StaffCallCreatedEvent event = StaffCallCreatedEvent.builder()
                        .callId(UUID.randomUUID())
                        .sessionId(order.getSession().getId())
                        .tableId(order.getTable() != null ? order.getTable().getId() : null)
                        .tableNumber(order.getTable() != null ? order.getTable().getNumber() : null)
                        .callType("PAYMENT_QR_EXPIRED") 
                        .calledAt(LocalDateTime.now())
                        .build();
                applicationEventPublisher.publishEvent(event);
            }
        } catch (Exception e) {
            log.error("PayOS Sync Job Error: Cập nhật lỗi cho đơn hàng {}", order.getId(), e);
        }
    }
}
