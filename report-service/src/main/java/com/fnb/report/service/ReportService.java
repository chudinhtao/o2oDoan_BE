package com.fnb.report.service;

import com.fnb.common.dto.ApiResponse;
import com.fnb.common.dto.PageResponse;
import com.fnb.report.dto.*;
import com.fnb.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;
    private final com.fnb.report.util.ExcelExporter excelExporter;

    public byte[] exportInventoryVariance(LocalDate from, LocalDate to) throws java.io.IOException {
        PageResponse<InventoryVarianceDto> pageData = getInventoryVarianceReport(from, to, 0, Integer.MAX_VALUE);
        return excelExporter.exportInventoryVariance(pageData.getContent());
    }

    public List<RevenueDto> getRevenueReport(LocalDate from, LocalDate to) {
        log.info("Fetching revenue report from {} to {}", from, to);
        return reportRepository.getRevenueReport(from, to);
    }

    public ProfitLossDto getProfitLossSummary(LocalDate from, LocalDate to) {
        log.info("Generating P&L Summary from {} to {}", from, to);
        
        List<RevenueDto> revenueData = reportRepository.getRevenueReport(from, to);
        BigDecimal totalNetRevenue = revenueData.stream()
                .map(RevenueDto::getNetRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalTax = revenueData.stream()
                .map(r -> r.getTaxAmount() != null ? r.getTaxAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        LocalDateTime startDt = from.atStartOfDay();
        LocalDateTime endDt = to.atTime(23, 59, 59);
        
        BigDecimal totalCogs = BigDecimal.ZERO;
        BigDecimal totalWaste = BigDecimal.ZERO;
        
        try {
            ProfitLossDto summary = reportRepository.getInventorySummary(startDt, endDt);
            if (summary != null) {
                totalCogs = summary.getTotalCogs();
                totalWaste = summary.getTotalWaste();
            }
        } catch (Exception e) {
            log.error("Failed to fetch inventory data for P&L: {}", e.getMessage());
        }
        
        BigDecimal grossProfit = totalNetRevenue.subtract(totalCogs);
        BigDecimal netProfit = grossProfit.subtract(totalWaste);
        
        BigDecimal margin = BigDecimal.ZERO;
        if (totalNetRevenue.compareTo(BigDecimal.ZERO) > 0) {
            margin = netProfit.multiply(BigDecimal.valueOf(100))
                    .divide(totalNetRevenue, 2, RoundingMode.HALF_UP);
        }
        
        return ProfitLossDto.builder()
                .startDate(from)
                .endDate(to)
                .totalRevenue(totalNetRevenue) // Revenue ở đây là Net Revenue
                .totalTax(totalTax)
                .totalCogs(totalCogs)
                .totalWaste(totalWaste)
                .grossProfit(grossProfit)
                .netProfit(netProfit)
                .profitMargin(margin)
                .build();
    }

    public PageResponse<InventoryVarianceDto> getInventoryVarianceReport(LocalDate from, LocalDate to, int page, int size) {
        log.info("Generating Inventory Variance (TvA) from {} to {} page {} size {}", from, to, page, size);
        
        LocalDateTime startDt = from.atStartOfDay();
        LocalDateTime endDt = to.atTime(23, 59, 59);
        
        try {
            return reportRepository.getInventoryVariance(startDt, endDt, page, size);
        } catch (Exception e) {
            log.error("Failed to calculate TvA: {}", e.getMessage(), e);
            return PageResponse.of(List.of(), page, size, 0);
        }
    }

    public PageResponse<TopItemDto> getTopItems(LocalDate from, LocalDate to, String sortBy, int page, int size) {
        log.info("Fetching top items from {} to {} sortBy={} page={} size={}", from, to, sortBy, page, size);
        return reportRepository.getTopItems(from, to, sortBy, page, size);
    }

    public List<SourceDto> getRevenueBySource(LocalDate from, LocalDate to) {
        log.info("Fetching revenue by source from {} to {}", from, to);
        return reportRepository.getRevenueBySource(from, to);
    }

    public List<HourlyTrafficDto> getHourlyTraffic(LocalDate from, LocalDate to) {
        log.info("Fetching hourly traffic from {} to {}", from, to);
        return reportRepository.getHourlyTraffic(from, to);
    }

    public List<TableUsageDto> getTableUsage(LocalDate from, LocalDate to) {
        log.info("Fetching table usage from {} to {}", from, to);
        return reportRepository.getTableUsage(from, to);
    }

    public ShiftReportDto getCashierShiftReport(LocalDate shiftDate, java.util.UUID cashierId, java.util.UUID attendanceId) {
        if (shiftDate == null) shiftDate = LocalDate.now();
        return reportRepository.getCashierShiftReport(shiftDate, cashierId, attendanceId);
    }

    public PageResponse<TopWastedItemDto> getTopWastedItems(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching top wasted items from {} to {}", from, to);
        return reportRepository.getTopWastedItems(from.atStartOfDay(), to.atTime(23, 59, 59), page, size);
    }
    
    public PageResponse<StaffPerformanceDto> getStaffPerformance(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching staff performance from {} to {}", from, to);
        return reportRepository.getStaffPerformance(from, to, page, size);
    }

    public PageResponse<PromotionEffectivenessDto> getPromotionEffectiveness(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching promotion effectiveness from {} to {}", from, to);
        return reportRepository.getPromotionEffectiveness(from, to, page, size);
    }

    public PageResponse<StaffCallStatsDto> getStaffCallStats(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching staff call stats from {} to {}", from, to);
        return reportRepository.getStaffCallStats(from, to, page, size);
    }

    public PageResponse<KitchenPerformanceDto> getKitchenPerformance(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching kitchen performance from {} to {}", from, to);
        return reportRepository.getKitchenPerformance(from, to, page, size);
    }

    public PageResponse<CancelledOrderDrilldownDto> getCancelledOrderDrilldown(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching cancelled order drilldown from {} to {}", from, to);
        return reportRepository.getCancelledOrderDrilldown(from, to, page, size);
    }

    public PageResponse<ChefPerformanceDto> getChefPerformance(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching chef performance from {} to {}", from, to);
        return reportRepository.getChefPerformance(from, to, page, size);
    }

    public PageResponse<ServerPerformanceDto> getServerPerformance(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching server performance from {} to {}", from, to);
        return reportRepository.getServerPerformance(from, to, page, size);
    }

    public PageResponse<CategorySalesDto> getCategorySales(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching category sales from {} to {}", from, to);
        return reportRepository.getCategorySales(from, to, page, size);
    }

    public PageResponse<StaffTimesheetDto> getStaffTimesheet(LocalDate from, LocalDate to, int page, int size) {
        log.info("Fetching staff timesheet from {} to {}", from, to);
        return reportRepository.getStaffTimesheet(from, to, page, size);
    }

    public ReservationReportDto getReservationReport(LocalDate from, LocalDate to) {
        log.info("Fetching reservation report from {} to {}", from, to);
        return reportRepository.getReservationReport(from, to);
    }
}
