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
import com.fnb.common.security.JwtProperties;
import com.fnb.common.security.JwtUtil;
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

    // TODO: Temporary block to enforce password reset
    @jakarta.annotation.PostConstruct
    public void forceUpdateAllPasswords() {
        try {
            String encoded = passwordEncoder.encode("123456");
            java.util.List<User> users = userRepository.findAll();
            for (User u : users) {
                u.setPassword(encoded);
            }
            userRepository.saveAll(users);
            log.info("FORCED PASSWORD RESET TO 123456 FOR ALL USERS ({})", users.size());
        } catch (Exception e) {
            log.error("Failed to force reset passwords", e);
        }
    }

    // ─── Login ───────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 1. Tìm user
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("Username hoặc password không đúng"));

        // 2. Kiểm tra active
        if (!user.isActive()) {
            throw new BusinessException("Tài khoản đã bị vô hiệu hóa");
        }

        // 3. Verify password bằng BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("Username hoặc password không đúng");
        }

        // 4. Tạo access token — claims chứa userId và role
        //    Gateway sẽ đọc 2 claims này để forward X-User-Id, X-User-Role
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role",   user.getRole()
        );
        String accessToken  = jwtUtil.generateToken(claims, user.getUsername());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // 5. Lưu refresh token vào DB (xóa cũ trước)
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

    // ─── Refresh Token ────────────────────────────────────────────────────

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        // 1. Tìm token trong DB
        RefreshToken stored = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Refresh token không hợp lệ"));

        // 2. Kiểm tra hết hạn
        if (stored.isExpired()) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException("Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = stored.getUser();

        // 3. Tạo access token mới
        Map<String, Object> claims = Map.of(
                "userId", user.getId().toString(),
                "role",   user.getRole()
        );
        String newAccessToken = jwtUtil.generateToken(claims, user.getUsername());

        return LoginResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .role(user.getRole())
                .fullName(user.getFullName())
                .expiresIn(jwtProperties.getExpiry())
                .build();
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
