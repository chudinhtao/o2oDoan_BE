package com.fnb.menu.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.menu.dto.request.ProfileRequest;
import com.fnb.menu.dto.response.ProfileResponse;
import com.fnb.menu.service.RestaurantProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only endpoint: PUT /api/admin/profile
 * Yêu cầu JWT hợp lệ + Role ADMIN.
 */
@RestController
@RequestMapping("/api/admin/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminProfileController {

    private final RestaurantProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile()));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@RequestBody ProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.upsert(request)));
    }
}
