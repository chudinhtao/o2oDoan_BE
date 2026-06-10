package com.fnb.inventory.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.inventory.dto.request.StocktakeItemUpdateRequest;
import com.fnb.inventory.dto.response.StocktakeResponse;
import com.fnb.inventory.service.StocktakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/inventory/stocktakes")
@RequiredArgsConstructor
public class StocktakeController {

    private final StocktakeService stocktakeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> createSnapshot(
            @Valid @RequestBody com.fnb.inventory.dto.request.StocktakeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo kỳ kiểm kê thành công", stocktakeService.createSnapshot(request)));
    }



    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<PageResponse<StocktakeResponse>>> getStocktakes(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.getStocktakes(status, keyword, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(stocktakeService.getStocktake(id)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> updateCounts(
            @PathVariable UUID id, 
            @Valid @RequestBody List<StocktakeItemUpdateRequest> countRequests) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật số đếm thành công", stocktakeService.updateCounts(id, countRequests)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> cancelStocktake(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy kiểm kê thành công", stocktakeService.cancelStocktake(id)));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StocktakeResponse>> completeStocktake(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("Chốt sổ kiểm kê thành công", stocktakeService.completeStocktake(id)));
    }
}
