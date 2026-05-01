package com.fnb.order.controller;

import com.fnb.order.service.PayOSPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.webhooks.Webhook;

import java.util.Map;

@RestController
@RequestMapping("/api/payments/payos")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PayOSPaymentService payOSPaymentService;

    /**
     * Frontend gọi API này để hệ thống tạo mã QR động.
     * Trả về Checkout URL của PayOS.
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPaymentLink(
            @RequestParam java.util.UUID orderId,
            @RequestParam java.math.BigDecimal amount,
            @RequestParam(required = false) java.math.BigDecimal cashAmount,
            @RequestParam String sessionToken) {
        
        log.info("Request create QR Pay for Order ID: {} with Session: {}", orderId, sessionToken);
        
        try {
            // Siết chặt bảo mật: Kiểm tra xem đơn hàng này có thuộc về Session của khách đang gọi không
            String checkoutUrl = payOSPaymentService.createPaymentLink(orderId, sessionToken, amount, cashAmount);
            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
        } catch (org.springframework.security.access.AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * API này ĐẶC BIỆT DÀNH RIÊNG cho con bot của PayOS gọi vào một cách thầm kín.
     * Khi khách chuyển khoản thành công, PayOS sẽ bắn data vào đây.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, String>> handlePayOSWebhook(@RequestBody Webhook webhook) {
        log.info("Nhận được tín hiệu Webhook từ hệ thống PayOS!");
        try {
            payOSPaymentService.handleWebhook(webhook);
            
            // Bắt buộc phải phản hồi 200 OK để PayOS biết là mình đã nhận được thư.
            return ResponseEntity.ok(Map.of("message", "Webhook received successfully"));
        } catch (Exception e) {
            log.error("Lỗi khi xử lý Webhook: ", e);
            // Nếu lỗi bảo mật, phản hồi 400 Bad Request
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
