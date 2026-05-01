package com.fnb.report.controller;

import com.fnb.common.dto.ApiResponse;
import com.fnb.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenue")
    public ApiResponse<?> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo doanh thu thành công", reportService.getRevenueReport(from, to));
    }

    // F2: Thêm sortBy param
    @GetMapping("/top-items")
    public ApiResponse<?> getTopItems(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "QUANTITY") String sortBy) {
        return ApiResponse.ok("Lấy báo cáo món ăn bán chạy thành công",
                reportService.getTopItems(limit, from, to, sortBy));
    }

    @GetMapping("/by-source")
    public ApiResponse<?> getRevenueBySource(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo theo nguồn thành công", reportService.getRevenueBySource(from, to));
    }

    @GetMapping("/by-hour")
    public ApiResponse<?> getHourlyTraffic(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo theo khung giờ thành công", reportService.getHourlyTraffic(from, to));
    }

    @GetMapping("/tables")
    public ApiResponse<?> getTableUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo sử dụng bàn thành công", reportService.getTableUsage(from, to));
    }

    @GetMapping("/cashier-shift")
    public ApiResponse<?> getCashierShiftReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate) {
        return ApiResponse.ok("Lấy báo cáo chốt ca thành công", reportService.getCashierShiftReport(shiftDate));
    }

    // N2: Endpoint mới — Hiệu quả khuyến mãi
    @GetMapping("/promotion-effectiveness")
    public ApiResponse<?> getPromotionEffectiveness(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo hiệu quả khuyến mãi thành công",
                reportService.getPromotionEffectiveness(from, to));
    }

    // N3: Endpoint mới — Thống kê gọi nhân viên
    @GetMapping("/staff-calls")
    public ApiResponse<?> getStaffCallStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy thống kê gọi nhân viên thành công",
                reportService.getStaffCallStats(from, to));
    }

    // 1.4: Endpoint mới — Hiệu suất bếp (thời gian làm món, tỷ lệ trễ)
    @GetMapping("/kitchen-performance")
    public ApiResponse<?> getKitchenPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy hiệu suất bếp thành công",
                reportService.getKitchenPerformance(from, to));
    }

    // 1.4: Endpoint mới — Chi tiết đơn hủy theo lý do
    @GetMapping("/cancelled-drilldown")
    public ApiResponse<?> getCancelledOrderDrilldown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy chi tiết đơn hủy thành công",
                reportService.getCancelledOrderDrilldown(from, to));
    }
}
