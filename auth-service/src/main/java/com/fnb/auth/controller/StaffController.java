package com.fnb.auth.controller;

import com.fnb.auth.dto.request.CreateStaffRequest;
import com.fnb.auth.dto.response.UserResponse;
import com.fnb.auth.service.StaffService;
import com.fnb.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/staff")
@PreAuthorize("hasRole('ADMIN')")   // Toàn bộ controller chỉ ADMIN được dùng
@RequiredArgsConstructor
public class StaffController {

    private final StaffService staffService;

    // GET /api/admin/staff
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(staffService.getAllStaff()));
    }

    // POST /api/admin/staff
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @Valid @RequestBody CreateStaffRequest request) {
        UserResponse created = staffService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.ok("Tạo nhân viên thành công", created));
    }

    // PATCH /api/admin/staff/{id}
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID id,
            @RequestBody CreateStaffRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(staffService.updateStaff(id, request)));
    }

    // PATCH /api/admin/staff/{id}/toggle
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ApiResponse<UserResponse>> toggle(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái thành công", staffService.toggleActive(id)));
    }
}
