package com.fnb.auth.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String role;
    private String fullName;
    private String phone;
    private boolean isActive;
    // pinCode cố ý không expose ra ngoài API để bảo mật
}
