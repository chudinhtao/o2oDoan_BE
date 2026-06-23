package com.fnb.ai.controller;

import com.fnb.ai.agent.admin.AdminOrchestrator;
import com.fnb.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import lombok.Data;

/**
 * Admin AI Chat Controller.
 * Nhận request từ api-gateway (đã xác thực), forward sang AdminOrchestrator.
 * Session theo ngày: mỗi ngày tự động tạo phiên mới, AI không bị drift context.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AdminOrchestrator adminOrchestrator;

    private static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    @Data
    public static class ChatRequest {
        private String message;
        private String sessionId;
    }

    @Data
    public static class ChatResponse {
        private String reply;
        private String sessionId;

        public ChatResponse(String reply, String sessionId) {
            this.reply = reply;
            this.sessionId = sessionId;
        }
    }

    /**
     * POST /api/admin/ai/chat
     */
    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestBody ChatRequest request
    ) {
        log.info("[ADMIN-AI] Chat | userId={} | role={} | msg={}", userId, userRole, request.getMessage());

        if (!"ADMIN".equalsIgnoreCase(userRole)) {
            return ApiResponse.ok("Bạn không có quyền sử dụng tính năng Admin AI.", null);
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ApiResponse.ok("Vui lòng nhập câu hỏi để trợ lý AI hỗ trợ bạn.", null);
        }

        // Session theo ngày làm việc (VN timezone):
        // - Mỗi ngày tự động reset → AI luôn tươi, không bị drift context từ hôm qua
        // - Cùng ngày thì vẫn giữ lịch sử trong phiên làm việc
        String today = java.time.LocalDate.now(VN_ZONE).toString(); // "2026-06-18"
        String memoryId = "admin-" + (userId != null ? userId : "anonymous") + "-" + today;

        log.info("[ADMIN-AI] memoryId={}", memoryId);
        String reply = adminOrchestrator.processChat(memoryId, request.getMessage().trim());
        return ApiResponse.ok("Thành công", new ChatResponse(reply, memoryId));
    }
}
