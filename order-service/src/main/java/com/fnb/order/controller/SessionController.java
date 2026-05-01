package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.response.SessionResponse;
import com.fnb.order.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/open")
    public ResponseEntity<ApiResponse<SessionResponse>> openSession(@RequestBody Map<String, String> request) {
        String token = request.get("qrToken");
        return ResponseEntity.ok(ApiResponse.ok("Tạo/Join phiên thành công", sessionService.openSession(token)));
    }

    @PostMapping("/open/manual/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<SessionResponse>> openSessionManual(@PathVariable java.util.UUID tableId) {
        return ResponseEntity.ok(ApiResponse.ok("Mở bàn thành công", sessionService.openSessionByTableId(tableId)));
    }

    @PostMapping("/open/takeaway")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'STAFF')")
    public ResponseEntity<ApiResponse<SessionResponse>> openTakeawaySession() {
        return ResponseEntity.ok(ApiResponse.ok("Tạo phiên mang đi thành công", sessionService.openTakeawaySession()));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<SessionResponse>> getSession(@PathVariable String token) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getSessionCurrent(token)));
    }

    @GetMapping("/active/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<SessionResponse>> getActiveSession(@PathVariable java.util.UUID tableId) {
        return ResponseEntity.ok(ApiResponse.ok(sessionService.getActiveSessionByTableId(tableId)));
    }

    @PostMapping("/{token}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<Void>> closeSession(@PathVariable String token) {
        sessionService.closeSession(token);
        return ResponseEntity.ok(ApiResponse.ok("Đã đóng phiên bàn", null));
    }
}
