package com.fnb.report.service;

import com.fnb.report.dto.*;
import com.fnb.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    public List<RevenueDto> getRevenueReport(LocalDate from, LocalDate to) {
        log.info("Fetching revenue report from {} to {}", from, to);
        return reportRepository.getRevenueReport(from, to);
    }

    public List<TopItemDto> getTopItems(int limit, LocalDate from, LocalDate to, String sortBy) {
        log.info("Fetching top {} items from {} to {} sortBy={}", limit, from, to, sortBy);
        return reportRepository.getTopItems(limit, from, to, sortBy);
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

    public ShiftReportDto getCashierShiftReport(LocalDate shiftDate) {
        log.info("Fetching cashier shift report for date: {}", shiftDate);
        return reportRepository.getCashierShiftReport(shiftDate);
    }

    public List<PromotionEffectivenessDto> getPromotionEffectiveness(LocalDate from, LocalDate to) {
        log.info("Fetching promotion effectiveness from {} to {}", from, to);
        return reportRepository.getPromotionEffectiveness(from, to);
    }

    public List<StaffCallStatsDto> getStaffCallStats(LocalDate from, LocalDate to) {
        log.info("Fetching staff call stats from {} to {}", from, to);
        return reportRepository.getStaffCallStats(from, to);
    }

    // 1.4: Hiệu suất bếp
    public List<KitchenPerformanceDto> getKitchenPerformance(LocalDate from, LocalDate to) {
        log.info("Fetching kitchen performance from {} to {}", from, to);
        return reportRepository.getKitchenPerformance(from, to);
    }

    // 1.4: Chi tiết đơn hủy theo lý do
    public List<CancelledOrderDrilldownDto> getCancelledOrderDrilldown(LocalDate from, LocalDate to) {
        log.info("Fetching cancelled order drilldown from {} to {}", from, to);
        return reportRepository.getCancelledOrderDrilldown(from, to);
    }
}
