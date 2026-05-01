package com.fnb.ai.exception;

import com.fnb.common.dto.ApiResponse;
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
    public ApiResponse<String> handleAllAiExceptions(Exception ex) {
        log.error("[AI-SERVICE] Unhandled exception: {}", ex.getMessage(), ex);
        // data = lời xin lỗi → FE đọc response.data.data (nhất quán với toàn hệ thống)
        return ApiResponse.ok(APOLOGY_MESSAGE);
    }
}
