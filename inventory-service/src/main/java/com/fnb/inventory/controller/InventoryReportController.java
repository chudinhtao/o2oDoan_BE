package com.fnb.inventory.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.inventory.dto.response.LowStockItemResponse;
import com.fnb.inventory.dto.response.VarianceReportResponse;
import com.fnb.inventory.service.InventoryReportService;
import lombok.RequiredArgsConstructor;
import com.fnb.inventory.enums.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory/reports")
@RequiredArgsConstructor
public class InventoryReportController {

    private final InventoryReportService reportService;

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<LowStockItemResponse>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getLowStockItems()));
    }

    @GetMapping("/expiring")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<com.fnb.inventory.dto.response.ExpiringStockResponse>>> getExpiringStockItems(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getExpiringStockItems(days)));
    }

    @GetMapping("/variance")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VarianceReportResponse>> getVarianceReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getVarianceReport(startDate, endDate)));
    }

    @GetMapping("/dashboard-summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.fnb.inventory.dto.response.DashboardSummaryResponse>> getDashboardSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getDashboardSummary(startDate, endDate)));
    }

    @GetMapping("/trend")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<com.fnb.inventory.dto.response.InventoryTrendResponse>>> getTrendData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getTrendData(startDate, endDate)));
    }



    @GetMapping("/suggestions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<com.fnb.inventory.dto.response.InventorySuggestionResponse>>> getPurchaseSuggestions() {
        return ResponseEntity.ok(ApiResponse.ok(reportService.getPurchaseSuggestions()));
    }
}
