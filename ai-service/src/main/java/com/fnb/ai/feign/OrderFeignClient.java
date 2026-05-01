package com.fnb.ai.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

/**
 * Feign client gọi sang order-service để gọi nhân viên.
 * Header X-Session-Token được tự động forward bởi FeignHeaderInterceptor.
 */
@FeignClient(name = "order-service")
public interface OrderFeignClient {

    @PostMapping("/api/staff-calls")
    String callStaff(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody StaffCallBody body
    );

    record StaffCallBody(UUID sessionId, String callType, String message) {}
}
