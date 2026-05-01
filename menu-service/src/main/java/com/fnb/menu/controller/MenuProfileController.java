package com.fnb.menu.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.menu.dto.response.ProfileResponse;
import com.fnb.menu.service.RestaurantProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public endpoint: GET /api/menu/profile
 * Cho phép Customer App & POS lấy thông tin cửa hàng.
 */
@RestController
@RequestMapping("/api/menu/profile")
@RequiredArgsConstructor
public class MenuProfileController {

    private final RestaurantProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile()));
    }
}
