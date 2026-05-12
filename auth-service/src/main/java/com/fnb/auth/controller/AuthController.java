package com.fnb.auth.controller;

import com.fnb.auth.dto.request.LoginRequest;
import com.fnb.auth.dto.response.LoginResponse;
import com.fnb.auth.dto.response.UserResponse;
import com.fnb.auth.service.AuthService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /api/auth/login */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", authService.login(request)));
    }

    /** POST /api/auth/refresh */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(body.get("refreshToken"))));
    }

    /**
     * POST /api/auth/logout
     * Yêu cầu header: Authorization: Bearer <access_token>
     * Header X-User-Id được gateway inject (hoặc JwtDirectFilter nếu gọi thẳng).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }

        String token  = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        String userId = auth.getName();
        authService.logout(token, userId);

        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công", null));
    }

    /**
     * GET /api/auth/me
     * Principal được set bởi GatewayHeaderFilter hoặc JwtDirectFilter.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }

        String userId = auth.getName();
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(userId)));
    }
}
