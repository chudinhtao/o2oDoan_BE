package com.fnb.order.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Value("${internal.secret}")
    private String internalSecret;

    @Override
    public void apply(RequestTemplate template) {
        // ALWAYS attach X-Internal-Secret for inter-service auth
        if (internalSecret != null) {
            template.header("X-Internal-Secret", internalSecret);
        }

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
}
