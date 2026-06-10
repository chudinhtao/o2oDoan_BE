package com.fnb.inventory.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Đọc userId từ SecurityContext (đã được GatewayHeaderFilter set từ X-User-Id header).
 * JPA Auditing dùng bean này để tự động fill @CreatedBy / @LastModifiedBy.
 */
@Component("auditorProvider")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.of("SYSTEM");
        }
        // auth.getName() trả về X-User-Id (UUID string) đã được GatewayHeaderFilter set
        return Optional.of(auth.getName());
    }
}
