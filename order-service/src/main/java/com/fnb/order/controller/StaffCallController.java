package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.request.StaffCallRequest;
import com.fnb.order.dto.response.StaffCallResponse;
import com.fnb.order.service.StaffCallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/staff-calls")
@RequiredArgsConstructor
public class StaffCallController {

    private final StaffCallService staffCallService;

    // Mobile App (Khách vẫy gọi phục vụ)
    @PostMapping
    public ApiResponse<String> requestSupport(
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody StaffCallRequest request) {
        staffCallService.createCall(sessionToken, request);
        return ApiResponse.ok("Đã gọi phục vụ thành công. Xin vui lòng đợi trong giây lát.", null);
    }

    // POS / Admin (Thu ngân xem các bàn đang vẫy)
    @GetMapping("/active")
    public ApiResponse<List<StaffCallResponse>> getActiveCalls() {
        return ApiResponse.ok(staffCallService.getActiveCalls());
    }

    // POS / Admin (Nhân viên ra xác nhận đã xử lý xong)
    @PutMapping("/{id}/resolve")
    public ApiResponse<String> resolveCall(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID id) {
        UUID resolvedBy = (userId != null) ? UUID.fromString(userId) : null;
        staffCallService.resolveCall(id, resolvedBy);
        return ApiResponse.ok("Đã đánh dấu xử lý xong", null);
    }
}
