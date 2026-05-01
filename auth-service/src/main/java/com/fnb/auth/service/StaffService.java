package com.fnb.auth.service;

import com.fnb.auth.dto.request.CreateStaffRequest;
import com.fnb.auth.dto.response.UserResponse;
import com.fnb.auth.entity.User;
import com.fnb.auth.repository.UserRepository;
import com.fnb.common.exception.BusinessException;
import com.fnb.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> getAllStaff() {
        return userRepository.findAll().stream()
                .filter(u -> !u.getRole().equals("ADMIN")) // Không liệt kê ADMIN
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse createStaff(CreateStaffRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username '" + request.getUsername() + "' đã tồn tại");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .fullName(request.getFullName())
                .build();

        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateStaff(UUID id, CreateStaffRequest request) {
        User user = findById(id);
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse toggleActive(UUID id) {
        User user = findById(id);
        user.setActive(!user.isActive());
        return toResponse(userRepository.save(user));
    }

    private User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên không tồn tại"));
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .fullName(user.getFullName())
                .isActive(user.isActive())
                .build();
    }
}
