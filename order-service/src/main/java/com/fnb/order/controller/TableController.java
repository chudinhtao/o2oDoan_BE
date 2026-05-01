package com.fnb.order.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.order.dto.request.MergeTableRequest;
import com.fnb.order.dto.request.TableActionRequest;
import com.fnb.order.dto.response.TableResponse;
import com.fnb.order.service.TableActionService;
import com.fnb.order.service.TableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
public class TableController {

    private final TableService tableService;
    private final TableActionService tableActionService;

    /** GET /api/tables – Danh sách bàn với filter + phân trang */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<TableResponse>>> getAllTables(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getAllTables(keyword, status, isActive, page, size)));
    }

    /** GET /api/tables/pos – Danh sách bàn cho màn hình POS (không phân trang, kèm session) */
    @GetMapping("/pos")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<java.util.List<com.fnb.order.dto.response.PosTableResponse>>> getAllTablesForPos() {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getAllForPos()));
    }


    /** GET /api/tables/{id} – Chi tiết bàn */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TableResponse>> getTable(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getTable(id)));
    }

    /** PATCH /api/tables/{id}/clean-done – Xác nhận dọn xong (CLEANING → FREE) */
    @PatchMapping("/{id}/clean-done")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<Void>> markCleaned(@PathVariable UUID id) {
        tableService.markAsCleaned(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã dọn sọn", null));
    }


    /** POST /api/tables/merge – Gộp nhiều bàn vào 1 bàn đích */
    @PostMapping("/merge")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<Void>> mergeTables(@Valid @RequestBody MergeTableRequest request) {
        tableActionService.mergeTables(request.getSourceTableIds(), request.getTargetTableId());
        return ResponseEntity.ok(ApiResponse.ok("Đã gộp bàn thành công.", null));
    }

    /** POST /api/tables/transfer – Chuyển bàn */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<Void>> transferTable(@Valid @RequestBody TableActionRequest request) {
        tableActionService.transferTable(request.getSourceTableId(), request.getTargetTableId());
        return ResponseEntity.ok(ApiResponse.ok("Đã chuyển bàn thành công.", null));
    }

    /**
     * GET /api/tables/zones
     * Lấy danh sách Zone duy nhất trong nhà hàng.
     * Dùng cho Multi-select Dropdown Zone trên Server Mobile App.
     * Không cần phân quyền (Server cũng cần gọi endpoint này).
     */
    @GetMapping("/zones")
    public ResponseEntity<ApiResponse<java.util.List<String>>> getDistinctZones() {
        return ResponseEntity.ok(ApiResponse.ok(tableService.getDistinctZones()));
    }
}
