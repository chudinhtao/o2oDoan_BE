package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final OrderService orderService;

    /**
     * API này giả lập việc nhận Webhook từ ngân hàng (VD: SePay, PayOS, Casso)
     * Ngân hàng sẽ gọi API này bất cứ khi nào có tiền chuyển vào tài khoản.
     */
    @PostMapping("/bank-transfer")
    public ResponseEntity<ApiResponse<String>> handleBankWebhook(@RequestBody Map<String, Object> payload) {
        log.info("Nhận webhook từ Ngân hàng: {}", payload);
        
        try {
            // Giả lập cấu trúc webhook của Casso/SePay
            // Content chuyển khoản: "Thanh toan don hang abc-xyz..."
            String transferContent = payload.containsKey("content") ? payload.get("content").toString() : "";
            BigDecimal amount = new BigDecimal(payload.containsKey("amount") ? payload.get("amount").toString() : "0");
            String refCode = payload.containsKey("referenceCode") ? payload.get("referenceCode").toString() : "";

            orderService.handleBankWebhook(transferContent, amount, refCode);
            return ResponseEntity.ok(ApiResponse.ok("Xử lý Webhook thành công", null));
        } catch (Exception e) {
            log.error("Lỗi khi xử lý Webhook ngân hàng: ", e);
            // Vẫn trả về 200 OK để ngân hàng không gọi lại (tùy vào logic webhook provider)
            return ResponseEntity.ok(ApiResponse.error("Lỗi nội bộ: " + e.getMessage()));
        }
    }
}
