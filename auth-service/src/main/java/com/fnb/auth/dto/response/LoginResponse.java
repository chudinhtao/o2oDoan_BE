package com.fnb.auth.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String id;          // UUID của user
    private String username;
    private String accessToken;
    private String refreshToken;
    private String role;
    private String fullName;
    private long expiresIn;     // giây
}
