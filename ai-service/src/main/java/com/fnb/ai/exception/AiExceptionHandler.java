package com.fnb.ai.exception;

import com.fnb.ai.controller.AdminAiController;
import com.fnb.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Bảo vệ Frontend Chat UI khỏi bị sập.
 * Mọi exception từ Grok API / JDBC / Feign đều bị bắt ở đây
 * và trả về HTTP 200 với lời xin lỗi thân thiện.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.fnb.ai.controller")
public class AiExceptionHandler {

    private static final String APOLOGY_MESSAGE =
            "Dạ, hiện tại trợ lý đang bận xử lý thông tin, anh/chị thông cảm đợi em một lát " +
            "hoặc gọi trực tiếp nhân viên gần nhất giúp em nhé! 🙏";

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<?> handleAllAiExceptions(Exception ex, HttpServletRequest request) {
        log.warn("[AI-SERVICE] Đã xử lý lỗi ngầm tại {}: {} - Trả về tin nhắn thân thiện cho FE", request.getRequestURI(), ex.getMessage());
        
        String path = request.getRequestURI();
        
        // Nếu là Admin AI đang gọi, trả về object ChatResponse để FE không bị crash khi đọc data.reply
        if (path != null && path.startsWith("/api/admin/ai")) {
            return ApiResponse.ok("Thành công", new AdminAiController.ChatResponse(APOLOGY_MESSAGE, null));
        }

        // Mặc định (Customer AI) thì trả về String (nhất quán với CustomerAiController)
        // Để chắc chắn FE lấy được chuỗi, dùng data = APOLOGY_MESSAGE
        return ApiResponse.ok("Thành công", APOLOGY_MESSAGE);
    }
}
