package com.fnb.ai.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@FeignClient(name = "kds-service")
public interface KdsFeignClient {

    /**
     * Lấy danh sách vé bếp (KDS tickets) đang hoạt động hoặc mới hoàn thành.
     * API nay tra ve raw List<Map<String, Object>>.
     */
    @GetMapping("/api/kds/tickets/active")
    List<Map<String, Object>> getActiveTickets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startFrom);
}
