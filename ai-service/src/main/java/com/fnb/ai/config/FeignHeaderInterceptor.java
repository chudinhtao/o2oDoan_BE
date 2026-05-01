package com.fnb.ai.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Sao chép headers từ request đến vào mọi FeignClient call.
 * Đảm bảo order-service nhận đúng X-Session-Token / X-User-Id / X-User-Role
 * mà không bị 401 Unauthorized.
 */
@Configuration
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();

        // Forward Session Token (Customer flow)
        String sessionToken = request.getHeader("X-Session-Token");
        if (sessionToken != null) {
            template.header("X-Session-Token", sessionToken);
        }

        // Forward Gateway-injected identity headers (Admin flow)
        String userId = request.getHeader("X-User-Id");
        String userRole = request.getHeader("X-User-Role");
        if (userId != null)   template.header("X-User-Id", userId);
        if (userRole != null) template.header("X-User-Role", userRole);
    }

    // Xóa method @Bean bị trùng lặp vì class đã được annotate bằng @Configuration
}
