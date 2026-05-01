package com.fnb.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@Slf4j
public class PayOSConfig {

    @Value("${payos.client-id}")
    private String clientId;

    @Value("${payos.api-key}")
    private String apiKey;

    @Value("${payos.checksum-key}")
    private String checksumKey;

    @Bean
    public PayOS payOS() {
        if ("mock".equals(clientId) || clientId == null || clientId.trim().isEmpty()) {
            log.warn("PayOS keys are missing or set to mock. Initializing a mock PayOS instance (this will not actually call PayOS APIs).");
            // Khởi tạo PayOS Dummy nếu chưa có key thật để ứng dụng không bị crash.
            return new PayOS("mockId", "mockKey", "mockChecksum");
        }
        log.info("PayOS has been initialized with actual client ID.");
        return new PayOS(clientId, apiKey, checksumKey);
    }
}
