package com.fnb.inventory.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.inventory.dto.request.QuickGrnRequest;
import com.fnb.inventory.dto.request.StockTransactionRequest;
import com.fnb.inventory.dto.response.StockTransactionResponse;
import com.fnb.inventory.service.StockTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory/transactions")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService txService;

    /**
     * Xuất hủy nguyên liệu 1-chạm (bể vỡ, hỏng, hết hạn).
     * KITCHEN và ADMIN được phép dùng.
     */
    @PostMapping("/waste")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN', 'SERVER', 'CASHIER')")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> recordWaste(
            @Valid @RequestBody StockTransactionRequest request) {
        request.setTransactionType(TransactionType.OUT_WASTE);
        if (request.getQuantityChange().signum() > 0) {
            request.setQuantityChange(request.getQuantityChange().negate());
        }
        return ResponseEntity.ok(ApiResponse.ok("Xuất hủy thành công", txService.recordTransaction(request)));
    }

    /**
     * Kill-switch thủ công: KITCHEN bấm "Báo hết" để block nguyên liệu ngay lập tức.
     * Ghi 1 transaction MANUAL_BLOCK với quantityChange = -currentStock để về 0,
     * trigger InventoryOutOfStockEvent → ẩn tất cả món liên quan trên QR menu.
     */
    @PostMapping("/items/{itemId}/kill-switch")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN', 'SERVER', 'CASHIER', 'POS', 'KDS')")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> killSwitch(
            @PathVariable UUID itemId,
            @RequestParam(defaultValue = "Bếp báo hết nguyên liệu") String reason) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Kill-switch kích hoạt thành công",
                txService.applyKillSwitch(itemId, reason)));
    }

    /**
     * Mở lại Kill-switch: KITCHEN/ADMIN mở lại món sau khi đã có hàng.
     * Ghi 1 transaction ADJUSTMENT với giá trị thực tế,
     * trigger InventoryInStockEvent → mở lại tất cả món liên quan.
     */
    @PostMapping("/items/{itemId}/restore-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN', 'SERVER', 'CASHIER', 'POS', 'KDS')")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> restoreStock(
            @PathVariable UUID itemId,
            @RequestParam BigDecimal quantity,
            @RequestParam(defaultValue = "Bếp xác nhận có hàng trở lại") String reason) {
        StockTransactionRequest req = StockTransactionRequest.builder()
                .itemId(itemId)
                .transactionType(TransactionType.ADJUSTMENT)
                .quantityChange(quantity.abs())
                .reason(reason)
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Mở lại nguyên liệu thành công", txService.recordTransaction(req)));
    }

    /**
     * Nhập nhanh (Quick GRN): Nhập kho 2-chạm không cần PO.
     * Dùng khi hàng về gấp lúc đông khách, kế toán sẽ khớp sau.
     * ADMIN và SERVER (quản lý quầy) được phép dùng.
     */
    @PostMapping("/quick-grn")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVER')")
    public ResponseEntity<ApiResponse<StockTransactionResponse>> quickGrn(
            @Valid @RequestBody QuickGrnRequest request) {
        StockTransactionRequest txRequest = StockTransactionRequest.builder()
                .itemId(request.getItemId())
                .transactionType(TransactionType.IN_QUICK)
                .quantityChange(request.getQuantity().abs())
                .unitPriceAtTransaction(request.getUnitPrice())
                .reason("Nhập nhanh: " + (request.getNote() != null ? request.getNote() : "Không có ghi chú"))
                .lotNumber(request.getLotNumber())
                .expiryDate(request.getExpiryDate())
                .build();
        return ResponseEntity.ok(ApiResponse.ok("Nhập nhanh thành công", txService.recordTransaction(txRequest)));
    }

    /**
     * Luân chuyển nội bộ (Internal Transfer).
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'SERVER')")
    public ResponseEntity<ApiResponse<java.util.List<StockTransactionResponse>>> internalTransfer(
            @Valid @RequestBody com.fnb.inventory.dto.request.InternalTransferRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Chuyển kho thành công", txService.transferStock(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<StockTransactionResponse>>> getTransactions(
            @RequestParam(required = false) UUID itemId,
            @RequestParam(required = false) TransactionType transactionType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(txService.getTransactionHistory(itemId, transactionType, startDate, endDate, page, size)));
    }
}

