package com.fnb.ai.controller;

import com.fnb.ai.agent.admin.AdminOrchestrator;
import com.fnb.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import java.util.UUID;

/**
 * Admin AI Chat Controller.
 * Nhận request từ api-gateway (đã xác thực), forward sang AdminOrchestrator.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ai")
@RequiredArgsConstructor
public class AdminAiController {

    private final AdminOrchestrator adminOrchestrator;

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

        // Dùng sessionId từ frontend hoặc tạo mới nếu chưa có
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank() 
                ? request.getSessionId() 
                : UUID.randomUUID().toString();
        
        // Cần đảm bảo memory ID là duy nhất cho mỗi session của mỗi admin
        String memoryId = "admin-" + (userId != null ? userId : "anonymous") + "-" + sessionId;
        
        String reply = adminOrchestrator.processChat(memoryId, request.getMessage().trim());
        return ApiResponse.ok("Thành công", new ChatResponse(reply, sessionId));
    }
}
