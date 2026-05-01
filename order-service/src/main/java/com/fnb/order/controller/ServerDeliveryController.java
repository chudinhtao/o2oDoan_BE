package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.request.ServeItemsRequest;
import com.fnb.order.dto.response.ServerKpiResponse;
import com.fnb.order.dto.response.StaffCallResponse;
import com.fnb.order.dto.response.TicketDeliveryDto;
import com.fnb.order.service.ServerDeliveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Controller dành riêng cho role SERVER.
 * Tất cả endpoint đều yêu cầu X-User-Id header (được Gateway inject sau khi xác thực JWT).
 *
 * Base URL: /api/orders/server
 */
@RestController
@RequestMapping("/api/orders/server")
@RequiredArgsConstructor
public class ServerDeliveryController {

    private final ServerDeliveryService serverDeliveryService;

    // ===== DELIVERY ENDPOINTS =====

    /**
     * GET /api/orders/server/deliveries?zones=Tầng 1,Tầng 2
     * Lấy danh sách khay đồ ăn cần bưng, group by bàn.
     * zones: chuỗi cách nhau dấu phẩy. Bỏ trống = All Zones.
     */
    @GetMapping("/deliveries")
    public ApiResponse<List<TicketDeliveryDto>> getPendingDeliveries(
            @RequestParam(required = false) String zones) {
        List<String> zoneList = parseZones(zones);
        return ApiResponse.ok(serverDeliveryService.getPendingDeliveries(zoneList));
    }

    /**
     * PUT /api/orders/server/deliveries/serve
     * Server bấm "Bưng ra": đánh dấu danh sách món là SERVED.
     */
    @PutMapping("/deliveries/serve")
    public ApiResponse<String> serveItems(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ServeItemsRequest request) {
        int count = serverDeliveryService.serveItems(request.getItemIds(), UUID.fromString(userId));
        return ApiResponse.ok(count + " món đã được đánh dấu là đã phục vụ.", null);
    }

    /**
     * PUT /api/orders/server/deliveries/unserve
     * Undo serve: Chỉ hợp lệ trong vòng 30 giây sau khi bấm BƯNG RA.
     */
    @PutMapping("/deliveries/unserve")
    public ApiResponse<String> unserveItems(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ServeItemsRequest request) {
        int count = serverDeliveryService.undoServe(request.getItemIds(), UUID.fromString(userId));
        return ApiResponse.ok("Đã hoàn tác " + count + " món về trạng thái chờ bưng.", null);
    }

    // ===== STAFF CALL ENDPOINTS =====

    /**
     * GET /api/orders/server/calls?zones=Tầng 1,Tầng 2
     * Lấy danh sách StaffCalls PENDING+ACCEPTED cho màn hình Server.
     * zones: Bỏ trống = All Zones.
     */
    @GetMapping("/calls")
    public ApiResponse<List<StaffCallResponse>> getActiveCalls(
            @RequestParam(required = false) String zones) {
        List<String> zoneList = parseZones(zones);
        return ApiResponse.ok(serverDeliveryService.getActiveServerCalls(zoneList));
    }

    /**
     * PUT /api/orders/server/calls/{id}/accept
     * Server tiếp nhận yêu cầu — dùng Optimistic Locking.
     * Nếu đã có người accept trước → 409 Conflict.
     */
    @PutMapping("/calls/{id}/accept")
    public ApiResponse<String> acceptCall(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @PathVariable UUID id) {
        serverDeliveryService.acceptCall(id, UUID.fromString(userId), userName);
        return ApiResponse.ok("Đã tiếp nhận yêu cầu.", null);
    }

    /**
     * PUT /api/orders/server/calls/{id}/resolve
     * Bấm hoàn thành tác vụ: chuyển ACCEPTED → RESOLVED.
     */
    @PutMapping("/calls/{id}/resolve")
    public ApiResponse<String> resolveCall(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        serverDeliveryService.resolveCall(id, UUID.fromString(userId));
        return ApiResponse.ok("Đã hoàn thành tác vụ.", null);
    }

    // ===== KPI ENDPOINT =====

    /**
     * GET /api/orders/server/kpi/today
     * Thống kê hiệu suất của Server hôm nay: tổng bưng, tổng xử lý, tgian trung bình.
     */
    @GetMapping("/kpi/today")
    public ApiResponse<ServerKpiResponse> getKpiToday(
            @RequestHeader("X-User-Id") String userId) {
        return ApiResponse.ok(serverDeliveryService.getKpiToday(UUID.fromString(userId)));
    }

    // ===== Helper =====

    /** Tách chuỗi "Tầng 1,Tầng 2" thành List. Trả null nếu rỗng. */
    private List<String> parseZones(String zones) {
        if (zones == null || zones.isBlank()) return null;
        return Arrays.asList(zones.split(","));
    }
}
