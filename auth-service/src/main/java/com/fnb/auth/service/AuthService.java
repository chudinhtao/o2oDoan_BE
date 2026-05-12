package com.fnb.auth.service;

import com.fnb.auth.dto.request.LoginRequest;
import com.fnb.auth.dto.response.LoginResponse;
import com.fnb.auth.dto.response.UserResponse;
import com.fnb.auth.entity.RefreshToken;
import com.fnb.auth.entity.User;
import com.fnb.auth.repository.RefreshTokenRepository;
import com.fnb.auth.repository.UserRepository;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import com.fnb.common.exception.UnauthorizedException;
import com.fnb.auth.security.JwtProperties;
import com.fnb.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    // ─── Login ───────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Username hoặc password không đúng"));

        if (!user.isActive()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Username hoặc password không đúng");
        }

        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role",   user.getRole()
        );
        String accessToken  = jwtUtil.generateToken(claims, user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // Xóa tất cả refresh token cũ — đảm bảo 1 user / 1 phiên
        refreshTokenRepository.deleteByUserId(user.getId());
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiry()))
                .build());

        log.info("Login success: username={}, role={}", user.getUsername(), user.getRole());

        return LoginResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .fullName(user.getFullName())
                .expiresIn(jwtProperties.getExpiry())
                .build();
    }

    // ─── Refresh Token (với Rotation) ────────────────────────────────────

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = stored.getUser();

        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role",   user.getRole()
        );
        String newAccessToken  = jwtUtil.generateToken(claims, user.getUsername());

        // Refresh Token Rotation: xóa token cũ, cấp token mới
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        refreshTokenRepository.delete(stored);
        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpiry()))
                .build());

        log.debug("Token refreshed (rotation): userId={}", user.getId());

        return LoginResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .role(user.getRole())
                .fullName(user.getFullName())
                .expiresIn(jwtProperties.getExpiry())
                .build();
    }

    // ─── Logout ──────────────────────────────────────────────────────────

    @Transactional
    public void logout(String accessToken, String userId) {
        // 1. Đưa Access Token vào Blacklist (Redis), TTL = thời gian sống còn lại
        tokenBlacklistService.blacklist(accessToken);

        // 2. Xóa Refresh Token khỏi DB
        try {
            refreshTokenRepository.deleteByUserId(UUID.fromString(userId));
        } catch (IllegalArgumentException e) {
            log.warn("Logout: userId không hợp lệ — {}", userId);
        }

        log.info("Logout success: userId={}", userId);
    }

    // ─── Me ──────────────────────────────────────────────────────────────

    public UserResponse getMe(String userId) {
        User user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        return toResponse(user);
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .isActive(user.isActive())
                .build();
    }
}
