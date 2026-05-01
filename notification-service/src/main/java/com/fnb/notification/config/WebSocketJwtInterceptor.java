package com.fnb.notification.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class WebSocketJwtInterceptor implements ChannelInterceptor {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            
            // 1. Kiểm tra JWT Token của Staff/Admin
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            log.debug("WS Connect - Authorization headers: {}", authHeaders);

            if (authHeaders != null && !authHeaders.isEmpty()) {
                String bearerToken = authHeaders.get(0);
                if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    try {
                        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
                        Claims claims = Jwts.parser()
                                .verifyWith(key)
                                .build()
                                .parseSignedClaims(token)
                                .getPayload();
                        
                        String role = claims.get("role", String.class);
                        String userId = claims.get("userId", String.class);
                        log.info("WS CONNECT SUCCESS - User: {}, Role: {}", userId, role);
                        
                        // Đính kèm thông tin Staff vào Session Principal để đảm bảo persistence qua các frame sau
                        final String principalName = userId + ":" + role;
                        accessor.setUser(() -> principalName);
                        
                        // Vẫn lưu vào SessionAttributes cho chắc chắn
                        if (accessor.getSessionAttributes() == null) {
                            accessor.setSessionAttributes(new java.util.HashMap<>());
                        }
                        accessor.getSessionAttributes().put("role", role);
                        accessor.getSessionAttributes().put("userId", userId);
                        
                        return message; // OK, qua chốt
                    } catch (Exception e) {
                        log.error("WS CONNECT ERROR - Invalid JWT token: {}", e.getMessage());
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                }
            }

            // 2. Kiểm tra Session Token của Khách hàng (Customer)
            List<String> sessionHeaders = accessor.getNativeHeader("X-Session-Token");
            log.debug("WS Connect - Session headers: {}", sessionHeaders);

            if (sessionHeaders != null && !sessionHeaders.isEmpty()) {
                String sessionToken = sessionHeaders.get(0);
                if (sessionToken != null && !sessionToken.trim().isEmpty()) {
                    log.info("WS CONNECT SUCCESS - Customer Session: {}", sessionToken);
                    
                    // Đính kèm vào Principal cho chắc chắn
                    final String principalName = sessionToken + ":CUSTOMER";
                    accessor.setUser(() -> principalName);

                    if (accessor.getSessionAttributes() == null) {
                        accessor.setSessionAttributes(new java.util.HashMap<>());
                    }

                    accessor.getSessionAttributes().put("role", "CUSTOMER");
                    accessor.getSessionAttributes().put("sessionToken", sessionToken);
                    return message; // OK, qua chốt
                }
            }

            // 3. Nếu không có cả hai -> Từ chối kết nối
            log.warn("WS CONNECT REJECTED - Missing Authentication headers");
            throw new IllegalArgumentException("No authentication token found");
        }

        // Bắt kênh Subscribe, từ chối quyền truy cập không hợp lệ
        if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            var sessionAttributes = accessor.getSessionAttributes();
            var principal = accessor.getUser();
            
            String role = null;
            if (sessionAttributes != null && sessionAttributes.containsKey("role")) {
                role = (String) sessionAttributes.get("role");
            } else if (principal != null) {
                // Fallback lấy từ Principal nếu session attributes bị lạc trôi
                String name = principal.getName();
                if (name != null && name.contains(":")) {
                    role = name.split(":")[1];
                }
            }
            
            String destination = accessor.getDestination();
            log.debug("WS SUBSCRIBE - Destination: {}, Role detected: {}", destination, role);
            
            if (destination != null) {
                // 1. KDS - Yêu cầu role KITCHEN hoặc ADMIN cho bất kỳ topic nào chứa "kds"
                if (destination.contains("kds")) {
                    if (!"KITCHEN".equals(role) && !"ADMIN".equals(role)) {
                        log.error("Access Denied to KDS channel: {} for role: {}", destination, role);
                        throw new IllegalArgumentException("Access Denied: Kitchen role required");
                    }
                }
                // 2. Staff & POS - Yêu cầu role CASHIER hoặc ADMIN
                // Chỉ kiểm tra nếu KHÔNG PHẢI là topic KDS (đã check ở trên) và chứa các từ khóa staff/pos/orders
                else if (destination.contains("staff") || destination.contains("pos") || 
                    (destination.contains("orders") && !destination.contains("/topic/sessions/"))) {
                    if (!"CASHIER".equals(role) && !"ADMIN".equals(role)) {
                        log.error("Access Denied to Staff/POS channel: {} for role: {}", destination, role);
                        throw new IllegalArgumentException("Access Denied: Staff/Cashier role required");
                    }
                }
                
                // 3. Customer Session - Yêu cầu cùng sessionToken
                if (destination.startsWith("/topic/sessions/")) {
                    if ("CUSTOMER".equals(role)) {
                        String mySessionToken = (String) accessor.getSessionAttributes().get("sessionToken");
                        if (!destination.contains(mySessionToken)) {
                            log.error("Customer session mismatch: {} trying to listen to {}", mySessionToken, destination);
                            throw new IllegalArgumentException("Access Denied for other sessions");
                        }
                    } else if (role == null) {
                        // Nếu không có role (không login/không session) thì không cho nghe
                        throw new IllegalArgumentException("Authentication required");
                    }
                }
            }
        }

        return message;
    }
}
