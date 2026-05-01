package com.fnb.ai.controller;

import com.fnb.ai.agent.customer.CustomerOrchestrator;
import com.fnb.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Customer AI Chat Controller.
 * Nhận request từ Frontend (khách quét QR tại bàn).
 * X-Session-Token được truyền lên từ FE khi scan QR.
 */
@Slf4j
@RestController
@RequestMapping("/api/customer/ai")
@RequiredArgsConstructor
public class CustomerAiController {

    private final CustomerOrchestrator orchestrator;

    /**
     * POST /api/customer/ai/chat
     * Header: X-Session-Token (UUID string)
     * Body: plain text - câu hỏi của khách
     */
    @PostMapping("/chat")
    public ApiResponse<String> chat(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody String userMessage
    ) {
        log.info("[CUSTOMER-AI] Chat request | session={} | msg={}", sessionToken, userMessage);
        String reply = orchestrator.processChat(sessionToken, userMessage);
        // data = câu trả lời AI → FE đọc response.data.data (nhất quán với toàn hệ thống)
        return ApiResponse.ok(reply);
    }
}
