package com.fnb.notification.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Interceptor chạy trong quá trình HTTP Handshake (Upgrade sang WebSocket).
 * Nhiệm vụ: Móc cookie 'sessionToken' ra và ném vào WebSocket Session Attributes.
 */
@Component
public class CustomHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        if (request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            Cookie[] cookies = servletRequest.getCookies();
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("sessionToken".equals(cookie.getName())) {
                        // Lưu token vào attributes để ChannelInterceptor dùng sau này
                        attributes.put("sessionToken", cookie.getValue());
                        attributes.put("role", "CUSTOMER");
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
