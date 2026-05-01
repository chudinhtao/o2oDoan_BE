package com.fnb.auth.controller;

import com.fnb.auth.dto.request.LoginRequest;
import com.fnb.auth.dto.response.LoginResponse;
import com.fnb.auth.dto.response.UserResponse;
import com.fnb.auth.service.AuthService;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.exception.UnauthorizedException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", authService.login(request)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(body.get("refreshToken"))));
    }

    /**
     * GET /api/auth/me
     * Principal (userId UUID string) được set bởi:
     *  - GatewayHeaderFilter  → khi đi qua API Gateway (có X-User-Id header)
     *  - JwtDirectFilter      → khi gọi thẳng port 8081 với Bearer token
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Chưa đăng nhập");
        }

        String userId = auth.getName(); // getName() = getPrincipal().toString()
        return ResponseEntity.ok(ApiResponse.ok(authService.getMe(userId)));
    }

}
