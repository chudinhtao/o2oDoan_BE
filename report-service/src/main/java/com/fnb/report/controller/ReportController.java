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

    @GetMapping("/profit-loss")
    public ApiResponse<?> getProfitLossSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo lợi nhuận ròng thành công", reportService.getProfitLossSummary(from, to));
    }

    @GetMapping("/inventory-variance")
    public ApiResponse<?> getInventoryVariance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy báo cáo chênh lệch định lượng thành công", reportService.getInventoryVarianceReport(from, to, page, size));
    }

    @GetMapping("/inventory-variance/export")
    public org.springframework.http.ResponseEntity<byte[]> exportInventoryVariance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws java.io.IOException {
        byte[] excelContent = reportService.exportInventoryVariance(from, to);
        
        String filename = "TvA_Report_" + from + "_to_" + to + ".xlsx";
        return org.springframework.http.ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelContent);
    }

    // F2: Thêm sortBy param
    @GetMapping("/top-items")
    public ApiResponse<?> getTopItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "QUANTITY") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy báo cáo món ăn bán chạy thành công",
                reportService.getTopItems(from, to, sortBy, page, size));
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate,
            @RequestParam(required = false) java.util.UUID cashierId,
            @RequestParam(required = false) java.util.UUID attendanceId) {
        return ApiResponse.ok("Lấy báo cáo chốt ca thành công", reportService.getCashierShiftReport(shiftDate, cashierId, attendanceId));
    }
    
    @GetMapping("/staff-performance")
    public ApiResponse<?> getStaffPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy thống kê hiệu suất nhân viên thành công", reportService.getStaffPerformance(from, to, page, size));
    }
    
    @GetMapping("/top-wasted")
    public ApiResponse<?> getTopWastedItems(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy báo cáo top nguyên liệu hao hụt thành công", reportService.getTopWastedItems(from, to, page, size));
    }

    // N2: Endpoint mới — Hiệu quả khuyến mãi
    @GetMapping("/promotion-effectiveness")
    public ApiResponse<?> getPromotionEffectiveness(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy báo cáo hiệu quả khuyến mãi thành công",
                reportService.getPromotionEffectiveness(from, to, page, size));
    }

    // N3: Endpoint mới — Thống kê gọi nhân viên
    @GetMapping("/staff-calls")
    public ApiResponse<?> getStaffCallStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy thống kê gọi nhân viên thành công",
                reportService.getStaffCallStats(from, to, page, size));
    }

    // 1.4: Endpoint mới — Hiệu suất bếp (thời gian làm món, tỷ lệ trễ)
    @GetMapping("/kitchen-performance")
    public ApiResponse<?> getKitchenPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy hiệu suất bếp thành công",
                reportService.getKitchenPerformance(from, to, page, size));
    }

    // 1.4: Endpoint mới — Chi tiết đơn hủy theo lý do
    @GetMapping("/cancelled-drilldown")
    public ApiResponse<?> getCancelledOrderDrilldown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy chi tiết đơn hủy thành công",
                reportService.getCancelledOrderDrilldown(from, to, page, size));
    }

    @GetMapping("/kpi/kitchen")
    public ApiResponse<?> getChefPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy KPI đầu bếp thành công",
                reportService.getChefPerformance(from, to, page, size));
    }

    @GetMapping("/kpi/server")
    public ApiResponse<?> getServerPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy KPI phục vụ thành công",
                reportService.getServerPerformance(from, to, page, size));
    }

    @GetMapping("/by-category")
    public ApiResponse<?> getCategorySales(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy doanh thu theo danh mục thành công",
                reportService.getCategorySales(from, to, page, size));
    }

    @GetMapping("/timesheet")
    public ApiResponse<?> getStaffTimesheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok("Lấy báo cáo chấm công và năng suất thành công",
                reportService.getStaffTimesheet(from, to, page, size));
    }

    @GetMapping("/reservations")
    public ApiResponse<?> getReservationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok("Lấy báo cáo đặt bàn thành công",
                reportService.getReservationReport(from, to));
    }
}
