package com.fnb.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketJwtInterceptor jwtInterceptor;

    public WebSocketConfig(WebSocketJwtInterceptor jwtInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Hỗ trợ kết nối raw WebSocket (thích hợp với API Gateway và STOMP.js client mới)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
                
        // Fallback SockJS nếu dùng thư viện cũ
        registry.addEndpoint("/ws/sockjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable a simple memory-based message broker to carry messages back to the client on destinations prefixed with "/topic" and "/queue"
        registry.enableSimpleBroker("/topic", "/queue");
        // applicationDestinationPrefixes dùng khi client bắn message lên Server (Ví dụ bắt đầu với /app)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Kích hoạt xác thực qua JWT và Session Token
        registration.interceptors(jwtInterceptor);
    }
}
