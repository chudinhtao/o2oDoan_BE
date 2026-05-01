package com.fnb.ai.feign;

import com.fnb.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Feign client gọi sang report-service để lấy dữ liệu báo cáo cho AdminReportAgent.
 * Tất cả endpoints đều READ-ONLY, không có side effect.
 */
@FeignClient(name = "report-service")
public interface ReportFeignClient {

    @GetMapping("/api/reports/revenue")
    ApiResponse<List<RevenueRow>> getRevenueReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/api/reports/top-items")
    ApiResponse<List<TopItemRow>> getTopItems(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "QUANTITY") String sortBy);

    @GetMapping("/api/reports/by-source")
    ApiResponse<List<SourceRow>> getRevenueBySource(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/api/reports/by-hour")
    ApiResponse<List<HourlyRow>> getHourlyTraffic(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/api/reports/tables")
    ApiResponse<List<TableUsageRow>> getTableUsage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/api/reports/cashier-shift")
    ApiResponse<ShiftReportRow> getCashierShiftReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate shiftDate);

    @GetMapping("/api/reports/promotion-effectiveness")
    ApiResponse<List<PromotionEffRow>> getPromotionEffectiveness(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    @GetMapping("/api/reports/staff-calls")
    ApiResponse<List<StaffCallRow>> getStaffCallStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    // 1.4: Hiệu suất bếp
    @GetMapping("/api/reports/kitchen-performance")
    ApiResponse<List<KitchenPerfRow>> getKitchenPerformance(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    // 1.4: Chi tiết đơn hủy
    @GetMapping("/api/reports/cancelled-drilldown")
    ApiResponse<List<CancelledRow>> getCancelledOrderDrilldown(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to);

    // ─── Response Records ─────────────────────────────────────────────────────

    record RevenueRow(
            String day,
            BigDecimal revenue,
            long totalOrders,
            BigDecimal avgOrderValue
    ) {}

    record TopItemRow(
            String itemName,
            long totalSold,
            BigDecimal revenue
    ) {}

    record SourceRow(
            String source,
            long totalOrders,
            BigDecimal revenue,
            double percentage,
            BigDecimal totalAllRevenue
    ) {}

    record HourlyRow(
            int hourOfDay,
            long orderCount,
            BigDecimal revenue,
            BigDecimal avgOrderValue
    ) {}

    record TableUsageRow(
            String tableNumber,
            String tableName,
            String zone,
            Integer capacity,
            long sessionsCount,
            BigDecimal totalRevenue,
            BigDecimal avgSessionMinutes
    ) {}

    record ShiftReportRow(
            String shiftDate,
            BigDecimal totalRevenue,
            long totalOrders,
            Map<String, BigDecimal> revenueByPaymentMethod,
            Map<String, Long> ordersByPaymentMethod,
            long cancelledOrders,
            BigDecimal cancelledRevenue
    ) {}

    record PromotionEffRow(
            String promotionCode,
            long orderCount,
            BigDecimal totalDiscountGiven,
            BigDecimal grossRevenue,
            BigDecimal avgOrderValue
    ) {}

    record StaffCallRow(
            String tableNumber,
            String callType,
            long callCount,
            BigDecimal avgResolveMinutes
    ) {}

    // 1.4: Records mới cho operational drill-down
    record KitchenPerfRow(
            String itemName,
            long totalTickets,
            BigDecimal avgPrepMinutes,
            long lateTickets,
            double lateRate
    ) {}

    record CancelledRow(
            String cancellationReason,
            long cancelCount,
            BigDecimal cancelledRevenue,
            double cancelRate
    ) {}
}
