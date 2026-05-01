package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.order.dto.request.TicketRequest;
import com.fnb.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import org.springframework.security.access.prepost.PreAuthorize;
import com.fnb.common.dto.PageResponse;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/tickets")
    public ApiResponse<String> submitTicket(
            @RequestHeader("X-Session-Token") String sessionToken,
            @Valid @RequestBody(required = false) TicketRequest request) {
        
        orderService.submitTicket(sessionToken, request);
        return ApiResponse.ok("Đã gửi yêu cầu gọi món thành công, đang chờ bếp xử lý.", null);
    }

    @GetMapping("/session")
    public ApiResponse<com.fnb.order.dto.response.OrderResponse> getOrderForSession(
            @RequestHeader("X-Session-Token") String sessionToken) {
        return ApiResponse.ok(orderService.getOrderBySessionToken(sessionToken));
    }

    @PatchMapping("/session/items/{itemId}/cancel")
    public ApiResponse<String> cancelItemByCustomer(
            @RequestHeader("X-Session-Token") String sessionToken,
            @PathVariable java.util.UUID itemId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        orderService.cancelItemByCustomer(sessionToken, itemId, reason);
        return ApiResponse.ok("Bạn đã huỷ món thành công.", null);
    }

    @PatchMapping("/session/tickets/{ticketId}/cancel")
    public ApiResponse<String> cancelTicketByCustomer(
            @RequestHeader("X-Session-Token") String sessionToken,
            @PathVariable java.util.UUID ticketId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        orderService.cancelTicketByCustomer(sessionToken, ticketId, reason);
        return ApiResponse.ok("Bạn đã huỷ phiếu yêu cầu thành công.", null);
    }

    @PostMapping("/request-payment")
    public ApiResponse<String> requestPayment(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String paymentMethod = (payload != null && payload.containsKey("paymentMethod")) 
                                ? payload.get("paymentMethod").toUpperCase() 
                                : "UNKNOWN";
        orderService.requestPayment(sessionToken, paymentMethod);
        return ApiResponse.ok("Đã gửi yêu cầu thanh toán (" + paymentMethod + ") đến quầy thu ngân.", null);
    }

    @PatchMapping("/session/promotion")
    public ApiResponse<String> applyPromotion(
            @RequestHeader("X-Session-Token") String sessionToken,
            @RequestBody java.util.Map<String, String> payload) {
        String code = payload != null ? payload.get("code") : null;
        orderService.applyPromotion(sessionToken, code);
        return ApiResponse.ok(org.springframework.util.StringUtils.hasText(code) ? "Áp dụng mã giảm giá thành công." : "Đã gỡ mã giảm giá.", null);
    }

    @GetMapping("/{id}")
    public ApiResponse<com.fnb.order.dto.response.OrderResponse> getOrderById(
            @PathVariable java.util.UUID id) {
        return ApiResponse.ok(orderService.getOrderById(id));
    }

    @PatchMapping("/{id}/promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<String> applyPromotionPOS(
            @PathVariable java.util.UUID id,
            @RequestBody java.util.Map<String, String> payload) {
        String code = payload != null ? payload.get("code") : null;
        orderService.applyPromotionById(id, code);
        return ApiResponse.ok(org.springframework.util.StringUtils.hasText(code) ? "Áp dụng mã thành công." : "Đã gỡ mã.", null);
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<PageResponse<com.fnb.order.dto.response.OrderResponse>> getOrderHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        String[] sortParams = sort.split(",");
        Sort sortObj = Sort.by(
                Sort.Direction.fromString(sortParams.length > 1 ? sortParams[1] : "desc"),
                sortParams[0]
        );
        Pageable pageable = PageRequest.of(page, size, sortObj);

        return ApiResponse.ok(orderService.getOrderHistory(status, source, search, startDate, endDate, pageable));
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<String> checkoutOrder(
            @PathVariable java.util.UUID id,
            @RequestBody(required = false) java.util.Map<String, Object> payload,
            @RequestHeader(value = "X-User-Id", required = false) String cashierIdStr) {
        
        boolean releaseTable = payload == null || !payload.containsKey("releaseTable") || (boolean) payload.get("releaseTable");
        String paymentMethod = payload != null ? (String) payload.get("paymentMethod") : "CASH";
        
        // Convert map detail to string if present
        String paymentDetail = null;
        if (payload != null && payload.containsKey("paymentDetail")) {
            Object detail = payload.get("paymentDetail");
            if (detail instanceof String) {
                paymentDetail = (String) detail;
            } else {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    paymentDetail = mapper.writeValueAsString(detail);
                } catch (Exception e) {
                    paymentDetail = detail.toString(); 
                }
            }
        }

        java.util.UUID cashierId = cashierIdStr != null ? java.util.UUID.fromString(cashierIdStr) : null;
        orderService.closeOrder(id, releaseTable, cashierId, paymentMethod, paymentDetail);
        
        return ApiResponse.ok("Dã chốt hóa đơn thành công" + (releaseTable ? " và giải phóng bàn" : " (giữ phiên bàn)"), null);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<String> cancelOrder(
            @PathVariable java.util.UUID id,
            @RequestBody(required = false) java.util.Map<String, String> payload,
            @RequestHeader(value = "X-User-Id", required = false) String cancellerIdStr) {
        String reason = payload != null ? payload.get("reason") : null;
        String note = payload != null ? payload.get("note") : null;
        java.util.UUID cancellerId = cancellerIdStr != null ? java.util.UUID.fromString(cancellerIdStr) : null;
        orderService.cancelOrder(id, reason, note, cancellerId);
        return ApiResponse.ok("Dã huỷ đơn hàng thành công", null);
    }

    @PatchMapping("/{id}/items/{itemId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'KITCHEN')")
    public ApiResponse<String> cancelItem(
            @PathVariable java.util.UUID id,
            @PathVariable java.util.UUID itemId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        orderService.cancelItem(id, itemId, reason);
        return ApiResponse.ok("Đã huỷ món thành công", null);
    }

    @PatchMapping("/{id}/tickets/{ticketId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<String> cancelTicket(
            @PathVariable java.util.UUID id,
            @PathVariable java.util.UUID ticketId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        orderService.cancelTicket(id, ticketId, reason);
        return ApiResponse.ok("Đã huỷ phiếu yêu cầu thành công", null);
    }

    @PatchMapping("/{id}/items/{itemId}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<String> returnItem(
            @PathVariable java.util.UUID id,
            @PathVariable java.util.UUID itemId,
            @RequestBody(required = false) java.util.Map<String, String> payload) {
        String reason = payload != null ? payload.get("reason") : null;
        orderService.returnItem(id, itemId, reason);
        return ApiResponse.ok("Đã trả món và hoàn tiền thành công", null);
    }

//    @PostMapping("/takeaway")
//    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'STAFF')")
//    public ApiResponse<String> createTakeawayOrder(
//            @Valid @RequestBody com.fnb.order.dto.request.TakeawayOrderRequest request,
//            org.springframework.security.core.Authentication authentication) {
//
//        String username = authentication != null ? authentication.getName() : "UNKNOWN";
//        orderService.createTakeawayOrder(request, username);
//        return ApiResponse.ok("Đã tạo đơn Take-away thành công", null);
//    }

    @GetMapping("/takeaway/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ApiResponse<java.util.List<com.fnb.order.dto.response.PosTableResponse>> getActiveTakeawayOrders() {
        return ApiResponse.ok("", orderService.getActiveTakeawayOrders());
    }
}
