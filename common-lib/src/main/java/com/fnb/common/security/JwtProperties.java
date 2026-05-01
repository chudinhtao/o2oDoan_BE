package com.fnb.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** JWT signing secret (min. 256-bit, base64 or plaintext) */
    private String secret = "fnb-self-ordering-super-secret-key-must-be-at-least-256bits-long";

    /** Access token expiry in seconds (default 1 hour) */
    private long expiry = 3600;

    /** Refresh token expiry in seconds (default 7 days) */
    private long refreshExpiry = 604800;
}
