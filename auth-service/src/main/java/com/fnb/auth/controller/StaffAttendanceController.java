package com.fnb.auth.controller;

import com.fnb.auth.service.StaffService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.security.GatewayHeaderFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth/staff")
@RequiredArgsConstructor
public class StaffAttendanceController {

    private final StaffService staffService;

    @PostMapping("/attendance/check-in")
    public ApiResponse<?> checkIn(HttpServletRequest request, @RequestBody(required = false) java.util.Map<String, String> body) {
        UUID userId = getUserIdFromRequest(request);
        String note = (body != null && body.containsKey("note")) ? body.get("note") : null;
        staffService.checkIn(userId, note);
        return ApiResponse.ok("Điểm danh vào ca thành công");
    }

    @PostMapping("/attendance/check-out")
    public ApiResponse<?> checkOut(HttpServletRequest request, @RequestBody(required = false) java.util.Map<String, String> body) {
        UUID userId = getUserIdFromRequest(request);
        String note = (body != null && body.containsKey("note")) ? body.get("note") : null;
        staffService.checkOut(userId, note);
        return ApiResponse.ok("Điểm danh tan ca thành công");
    }

    @org.springframework.web.bind.annotation.GetMapping("/attendance/current")
    public org.springframework.http.ResponseEntity<ApiResponse<?>> getCurrent(HttpServletRequest request) {
        try {
            UUID userId = getUserIdFromRequest(request);
            var log = staffService.getCurrentAttendance(userId);
            return org.springframework.http.ResponseEntity.ok(ApiResponse.ok(log)); // return null data if not clocked in yet
        } catch (RuntimeException e) {
            if ("NO_SCHEDULE".equals(e.getMessage())) {
                return org.springframework.http.ResponseEntity.status(org.springframework.http.HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Bạn không có ca làm việc nào được phân công hôm nay. Vui lòng liên hệ quản lý."));
            }
            throw e;
        }
    }

    private UUID getUserIdFromRequest(HttpServletRequest request) {
        String userIdStr = request.getHeader(GatewayHeaderFilter.HEADER_USER_ID);
        if (userIdStr == null) {
            throw new com.fnb.common.exception.BusinessException("Unauthorized: No user ID found in request headers");
        }
        return UUID.fromString(userIdStr);
    }
}
