package com.fnb.ai.tools;

import com.fnb.ai.feign.ReportFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Bộ công cụ báo cáo cho Admin AI Agent.
 * Tất cả đều READ-ONLY — gọi report-service qua Feign, format kết quả thành String súc tích cho LLM.
 */
@Slf4j
@Component("adminReportTools")
@RequiredArgsConstructor
public class AdminReportTools {

    private final ReportFeignClient reportFeignClient;

    @Tool("Lấy báo cáo tổng quan kinh doanh và SO SÁNH với kỳ trước. " +
          "Trả về: biến động doanh thu, biến động số đơn, hiệu suất phục vụ. " +
          "Dùng tool này để trả lời các câu hỏi 'tại sao', 'tình hình thế nào', 'so sánh với tuần/tháng trước'.")
    public String getExecutiveSummary(@P("Ngày bắt đầu") String from, @P("Ngày kết thúc") String to) {
        log.info("[ADMIN-TOOL] getExecutiveSummary from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            
            // Tính toán kỳ trước (Previous Period)
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(fromDate, toDate) + 1;
            LocalDate prevFromDate = fromDate.minusDays(daysBetween);
            LocalDate prevToDate = fromDate.minusDays(1);

            // Lấy dữ liệu kỳ này
            var currRev = reportFeignClient.getRevenueReport(fromDate, toDate).getData();
            var currCall = reportFeignClient.getStaffCallStats(fromDate, toDate).getData();
            
            // Lấy dữ liệu kỳ trước
            var prevRev = reportFeignClient.getRevenueReport(prevFromDate, prevToDate).getData();
            var prevCall = reportFeignClient.getStaffCallStats(prevFromDate, prevToDate).getData();

            // Tính toán chỉ số kỳ này
            java.math.BigDecimal currTotalRev = currRev != null ? currRev.stream().map(ReportFeignClient.RevenueRow::revenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : java.math.BigDecimal.ZERO;
            long currTotalOrders = currRev != null ? currRev.stream().mapToLong(ReportFeignClient.RevenueRow::totalOrders).sum() : 0;
            long currTotalCalls = currCall != null ? currCall.stream().mapToLong(ReportFeignClient.StaffCallRow::callCount).sum() : 0;

            // Tính toán chỉ số kỳ trước
            java.math.BigDecimal prevTotalRev = prevRev != null ? prevRev.stream().map(ReportFeignClient.RevenueRow::revenue).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add) : java.math.BigDecimal.ZERO;
            long prevTotalOrders = prevRev != null ? prevRev.stream().mapToLong(ReportFeignClient.RevenueRow::totalOrders).sum() : 0;

            // Tính % tăng trưởng
            String revGrowth = calculateGrowth(currTotalRev, prevTotalRev);
            String orderGrowth = calculateGrowth(currTotalOrders, prevTotalOrders);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 BÁO CÁO PHÂN TÍCH SO SÁNH (").append(from).append(" vs kỳ trước ").append(prevFromDate).append("):\n\n");
            sb.append("💰 Doanh thu: ").append(formatVnd(currTotalRev)).append(" (").append(revGrowth).append(")\n");
            sb.append("📦 Số đơn hàng: ").append(currTotalOrders).append(" đơn (").append(orderGrowth).append(")\n");
            sb.append("📣 Gọi nhân viên: ").append(currTotalCalls).append(" lượt\n");
            sb.append("⚖️ Tỷ lệ gọi/đơn: ").append(currTotalOrders > 0 ? String.format("%.2f", (double)currTotalCalls/currTotalOrders) : "0").append("\n");
            
            if (currTotalOrders > 0 && prevTotalOrders > 0) {
                double currAov = currTotalRev.doubleValue() / currTotalOrders;
                double prevAov = prevTotalRev.doubleValue() / prevTotalOrders;
                sb.append("💳 Giá trị đơn TB (AOV): ").append(formatVnd(java.math.BigDecimal.valueOf(currAov)))
                  .append(" (").append(calculateGrowth(currAov, prevAov)).append(")\n");
            }

            sb.append("\n👉 NHẬN ĐỊNH NHANH:\n");
            if (currTotalRev.compareTo(prevTotalRev) < 0 && currTotalOrders >= prevTotalOrders) {
                sb.append("- CẢNH BÁO: Số đơn tăng nhưng doanh thu giảm -> Khách đang chi tiêu ít hơn trên mỗi đơn.\n");
            }
            if (currTotalOrders > 0 && (double)currTotalCalls/currTotalOrders > 1.5) {
                sb.append("- VẬN HÀNH: Tỷ lệ gọi nhân viên cao bất thường (>1.5) -> Có thể do thiếu người hoặc phục vụ chậm.\n");
            }
            
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getExecutiveSummary error: {}", e.getMessage());
            return "Không thể lấy báo cáo so sánh.";
        }
    }

    private String calculateGrowth(java.math.BigDecimal current, java.math.BigDecimal previous) {
        if (previous == null || previous.compareTo(java.math.BigDecimal.ZERO) == 0) return "n/a";
        java.math.BigDecimal growth = current.subtract(previous)
                .divide(previous, 4, java.math.RoundingMode.HALF_UP)
                .multiply(java.math.BigDecimal.valueOf(100));
        return (growth.compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "") + String.format("%.1f", growth) + "%";
    }

    private String calculateGrowth(double current, double previous) {
        if (previous == 0) return "n/a";
        double growth = ((current - previous) / previous) * 100;
        return (growth >= 0 ? "+" : "") + String.format("%.1f", growth) + "%";
    }

    @Tool("Lấy báo cáo doanh thu theo từng ngày trong khoảng thời gian. " +
          "Trả về: ngày, tổng doanh thu, số đơn, giá trị đơn trung bình. " +
          "Dùng khi admin hỏi về doanh thu, tổng thu, số đơn theo ngày/tuần/tháng. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getRevenueSummary(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                    @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getRevenueSummary from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueReport(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu doanh thu trong khoảng " + from + " đến " + to + ".";
            }
            List<ReportFeignClient.RevenueRow> rows = res.getData();

            long totalOrders = rows.stream().mapToLong(ReportFeignClient.RevenueRow::totalOrders).sum();
            java.math.BigDecimal totalRevenue = rows.stream()
                    .map(ReportFeignClient.RevenueRow::revenue)
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

            StringBuilder sb = new StringBuilder();
            sb.append("📊 Doanh thu từ ").append(from).append(" đến ").append(to).append(":\n\n");
            for (var row : rows) {
                sb.append("• ").append(row.day())
                  .append(": ").append(formatVnd(row.revenue()))
                  .append(" (").append(row.totalOrders()).append(" đơn")
                  .append(", TB/đơn: ").append(formatVnd(row.avgOrderValue())).append(")\n");
            }
            sb.append("\n📌 Tổng: ").append(formatVnd(totalRevenue))
              .append(" — ").append(totalOrders).append(" đơn");
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRevenueSummary error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu doanh thu. Vui lòng thử lại sau.";
        }
    }

    @Tool("Lấy danh sách món ăn bán chạy nhất theo số lượng hoặc doanh thu. " +
          "Dùng khi admin hỏi: 'top món', 'món nào bán chạy nhất', 'món nào doanh thu cao'. " +
          "sortBy có thể là QUANTITY (mặc định) hoặc REVENUE. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getTopItems(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                              @P("Ngày kết thúc (yyyy-MM-dd)") String to, 
                              @P("Số lượng kết quả trả về (ví dụ: 5, 10). Phải là số nguyên.") int limit, 
                              @P("Sắp xếp theo QUANTITY hoặc REVENUE") String sortBy) {
        log.info("[ADMIN-TOOL] getTopItems from={} to={} limit={} sortBy={}", from, to, limit, sortBy);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            String sort = (sortBy != null && sortBy.equalsIgnoreCase("REVENUE")) ? "REVENUE" : "QUANTITY";
            var res = reportFeignClient.getTopItems(limit, fromDate, toDate, sort);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu món bán chạy trong khoảng " + from + " đến " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🏆 Top ").append(limit).append(" món ").append("REVENUE".equals(sort) ? "(theo doanh thu)" : "(theo số lượng)")
              .append(" từ ").append(from).append(" đến ").append(to).append(":\n\n");
            int rank = 1;
            for (var item : res.getData()) {
                sb.append(rank++).append(". ").append(item.itemName())
                  .append(" — ").append(item.totalSold()).append(" phần")
                  .append(", ").append(formatVnd(item.revenue())).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getTopItems error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu top món. Vui lòng thử lại sau.";
        }
    }

    @Tool("Lấy báo cáo doanh thu theo nguồn đặt hàng (QR, MANUAL). " +
          "Trả về: nguồn, số đơn, doanh thu, % trên tổng. " +
          "Dùng khi admin hỏi về kênh bán, QR chiếm bao nhiêu %, nguồn nào bán tốt. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getRevenueBySource(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                     @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getRevenueBySource from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueBySource(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu theo nguồn trong khoảng " + from + " đến " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📡 Doanh thu theo nguồn từ ").append(from).append(" đến ").append(to).append(":\n\n");
            for (var row : res.getData()) {
                sb.append("• ").append(row.source())
                  .append(": ").append(formatVnd(row.revenue()))
                  .append(" (").append(row.totalOrders()).append(" đơn")
                  .append(", chiếm ").append(row.percentage()).append("%)\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getRevenueBySource error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu theo nguồn. Vui lòng thử lại sau.";
        }
    }

    @Tool("Lấy báo cáo lưu lượng đơn hàng theo từng khung giờ trong ngày. " +
          "Trả về: giờ, số đơn, doanh thu, giá trị đơn TB. " +
          "Dùng khi admin hỏi: 'giờ nào đông nhất', 'giờ cao điểm', 'khung giờ nào bán nhiều'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getHourlyTraffic(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                   @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getHourlyTraffic from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getHourlyTraffic(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu theo giờ trong khoảng " + from + " đến " + to + ".";
            }

            // Tìm giờ cao điểm
            var peak = res.getData().stream()
                    .max(java.util.Comparator.comparingLong(ReportFeignClient.HourlyRow::orderCount))
                    .orElse(null);

            StringBuilder sb = new StringBuilder();
            sb.append("⏰ Lưu lượng theo khung giờ từ ").append(from).append(" đến ").append(to).append(":\n\n");
            for (var row : res.getData()) {
                sb.append("• ").append(String.format("%02d:00", row.hourOfDay()))
                  .append(" — ").append(row.orderCount()).append(" đơn")
                  .append(", ").append(formatVnd(row.revenue())).append("\n");
            }
            if (peak != null) {
                sb.append("\n🔥 Giờ cao điểm: ").append(String.format("%02d:00", peak.hourOfDay()))
                  .append(" với ").append(peak.orderCount()).append(" đơn");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getHourlyTraffic error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu theo giờ. Vui lòng thử lại sau.";
        }
    }

    @Tool("Lấy báo cáo hiệu suất từng bàn: số phiên, doanh thu, khu vực, sức chứa, thời gian ngồi TB. " +
          "Dùng khi admin hỏi: 'bàn nào doanh thu cao', 'khu nào hiệu quả', 'bàn nào ít khách'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getTableUsageReport(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                      @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getTableUsageReport from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getTableUsage(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu sử dụng bàn trong khoảng " + from + " đến " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🪑 Hiệu suất bàn từ ").append(from).append(" đến ").append(to).append(":\n\n");
            for (var row : res.getData()) {
                sb.append("• Bàn ").append(row.tableNumber())
                  .append(" [").append(row.zone() != null ? row.zone() : "?").append("]")
                  .append(": ").append(formatVnd(row.totalRevenue()))
                  .append(", ").append(row.sessionsCount()).append(" phiên");
                if (row.avgSessionMinutes() != null) {
                    sb.append(", TB ").append(row.avgSessionMinutes()).append(" phút/phiên");
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getTableUsageReport error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu sử dụng bàn. Vui lòng thử lại sau.";
        }
    }

    @Tool("Lấy báo cáo chốt ca chi tiết. Trả về: doanh thu, số đơn, đơn huỷ, doanh thu theo phương thức thanh toán. " +
          "Tham số shiftDate định dạng yyyy-MM-dd.")
    public ReportFeignClient.ShiftReportRow getCashierShiftReport(@P("Ngày cần xem báo cáo (yyyy-MM-dd)") String shiftDate) {
        log.info("[ADMIN-TOOL] getCashierShiftReport shiftDate={}", shiftDate);
        try {
            LocalDate date = LocalDate.parse(shiftDate);
            var res = reportFeignClient.getCashierShiftReport(date);
            return (res != null) ? res.getData() : null;
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getCashierShiftReport error: {}", e.getMessage());
            return null;
        }
    }

    @Tool("Lấy báo cáo hiệu quả của các chương trình khuyến mãi. Trả về: mã KM, số đơn dùng, tổng tiền giảm, doanh thu tạo ra. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public List<ReportFeignClient.PromotionEffRow> getPromotionEffectiveness(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                                                             @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getPromotionEffectiveness from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getPromotionEffectiveness(fromDate, toDate);
            return (res != null) ? res.getData() : null;
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getPromotionEffectiveness error: {}", e.getMessage());
            return null;
        }
    }

    @Tool("Lấy danh sách chi tiết lượt gọi nhân viên theo từng bàn. Trả về: số bàn, loại yêu cầu, số lần gọi, thời gian xử lý TB. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public List<ReportFeignClient.StaffCallRow> getStaffCallStats(@P("Ngày bắt đầu (yyyy-MM-dd)") String from, 
                                                                  @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getStaffCallStats from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getStaffCallStats(fromDate, toDate);
            return (res != null) ? res.getData() : null;
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getStaffCallStats error: {}", e.getMessage());
            return null;
        }
    }

    @Tool("Phân tích hiệu suất Menu (Menu Engineering) dựa trên doanh thu và số lượng bán. " +
          "Dùng để xác định món nào hiệu quả cao (Star) hoặc cần loại bỏ (Dog). " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public List<ReportFeignClient.TopItemRow> getMenuPerformanceAnalysis(@P("Ngày bắt đầu") String from, 
                                                                         @P("Ngày kết thúc") String to) {
        log.info("[ADMIN-TOOL] getMenuPerformanceAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getTopItems(50, fromDate, toDate, "QUANTITY");
            return (res != null) ? res.getData() : null;
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getMenuPerformanceAnalysis error: {}", e.getMessage());
            return null;
        }
    }

    @Tool("Lấy báo cáo hiệu suất bếp: thời gian làm từng món, số ticket bị trễ (>15 phút), tỷ lệ trễ. " +
          "Dùng khi admin hỏi: 'bếp đang chậm?', 'món nào mất nhiều thời gian nhất', 'bếp có quá tải không'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getKitchenPerformanceReport(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                              @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getKitchenPerformanceReport from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getKitchenPerformance(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu hiệu suất bếp trong khoảng " + from + " đến " + to +
                       ". Có thể chưa có ticket hoàn thành hoặc hệ thống KDS chưa ghi nhận.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🍳 HIỆU SUẤT BẾP từ ").append(from).append(" đến ").append(to).append(":\n\n");

            long totalLate = 0, totalTickets = 0;
            for (var row : res.getData()) {
                totalTickets += row.totalTickets();
                totalLate += row.lateTickets();
                sb.append("• ").append(row.itemName()).append(": ");
                if (row.avgPrepMinutes() != null) {
                    sb.append(row.avgPrepMinutes()).append(" phút/ticket");
                } else {
                    sb.append("N/A");
                }
                sb.append(" | ").append(row.lateTickets()).append(" trễ (").append(row.lateRate()).append("%)\n");
            }

            double overallLateRate = totalTickets > 0
                ? Math.round((double) totalLate / totalTickets * 1000.0) / 10.0 : 0;
            sb.append("\n📊 Tổng: ").append(totalTickets).append(" tickets | Trễ: ").append(totalLate)
              .append(" (").append(overallLateRate).append("%)\n");

            if (overallLateRate > 20) {
                sb.append("\n⚠️ CẢNH BÁO: Tỷ lệ trễ >").append(overallLateRate)
                  .append("% — Bếp đang quá tải hoặc thiếu nhân lực. Cân nhắc điều chỉnh menu giờ cao điểm.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getKitchenPerformanceReport error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu hiệu suất bếp. Vui lòng thử lại sau.";
        }
    }

    @Tool("Phân tích chi tiết đơn hàng bị hủy theo lý do. Trả về: nguyên nhân hủy, số lượng, doanh thu bị mất, tỷ lệ. " +
          "Dùng khi admin hỏi: 'tại sao có nhiều đơn hủy', 'đơn hủy do lý do gì', 'doanh thu bị mất do hủy'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getCancelledOrderAnalysis(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                            @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[ADMIN-TOOL] getCancelledOrderAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);
            var res = reportFeignClient.getCancelledOrderDrilldown(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có đơn hàng bị hủy trong khoảng " + from + " đến " + to + ". Vận hành tốt! ✅";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🚫 PHÂN TÍCH ĐƠN HỦY từ ").append(from).append(" đến ").append(to).append(":\n\n");

            java.math.BigDecimal totalLost = java.math.BigDecimal.ZERO;
            long totalCancelled = 0;
            for (var row : res.getData()) {
                totalCancelled += row.cancelCount();
                totalLost = totalLost.add(row.cancelledRevenue());
                sb.append("• [").append(row.cancellationReason()).append("] ")
                  .append(row.cancelCount()).append(" đơn")
                  .append(" | Mất: ").append(formatVnd(row.cancelledRevenue()))
                  .append(" (").append(row.cancelRate()).append("% tổng đơn)\n");
            }

            sb.append("\n💸 Tổng doanh thu bị mất: ").append(formatVnd(totalLost))
              .append(" (").append(totalCancelled).append(" đơn bị hủy)");
            return sb.toString();
        } catch (Exception e) {
            log.error("[ADMIN-TOOL] getCancelledOrderAnalysis error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu đơn hủy. Vui lòng thử lại sau.";
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }
}
