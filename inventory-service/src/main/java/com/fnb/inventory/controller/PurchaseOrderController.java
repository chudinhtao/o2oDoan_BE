package com.fnb.inventory.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.inventory.dto.request.GoodsReceiptRequest;
import com.fnb.inventory.dto.request.PurchaseOrderRequest;
import com.fnb.inventory.dto.response.PurchaseOrderResponse;
import com.fnb.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService poService;

    /** Tạo phiếu mới (DRAFT hoặc QUICK_GRN -> auto COMPLETED) */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPo(
            @Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo phiếu nhập thành công", poService.createPo(request)));
    }

    /** Chỉnh sửa phiếu DRAFT (chỉ dùng được khi status = DRAFT) */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateDraftPo(
            @PathVariable UUID id,
            @Valid @RequestBody PurchaseOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật phiếu nháp thành công", poService.updateDraftPo(id, request)));
    }

    /** DRAFT -> CONFIRMED (Chốt đơn gửi NCC, khóa không cho sửa) */
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> confirmPo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã chốt phiếu nhập kho", poService.confirmPo(id)));
    }

    /** CONFIRMED/PARTIAL_RECEIVED -> Nhận hàng thực tế (có thể nhiều lần) */
    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receivePo(
            @PathVariable UUID id,
            @Valid @RequestBody GoodsReceiptRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Nhận hàng thành công", poService.receivePo(id, request)));
    }

    /** PARTIAL_RECEIVED -> COMPLETED (Đóng phiếu khi NCC không giao thêm) */
    @PostMapping("/{id}/force-complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> forceCompletePo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã đóng phiếu nhập kho", poService.forceCompletePo(id)));
    }

    /** DRAFT/CONFIRMED -> CANCELLED */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelPo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy phiếu thành công", poService.cancelPo(id)));
    }

    /** Danh sách PO với bộ lọc */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseOrderResponse>>> getPos(
            @RequestParam(required = false) POStatus status,
            @RequestParam(required = false) POType type,
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
                poService.getPos(status, type, poNumber, startDate, endDate, page, size)));
    }

    /** Chi tiết 1 PO */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPo(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(poService.getPo(id)));
    }
}
