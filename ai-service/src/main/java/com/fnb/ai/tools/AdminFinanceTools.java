package com.fnb.ai.tools;

import com.fnb.ai.feign.ReportFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Comparator;

/**
 * Bo cong cu Tai Chinh cho Admin AI — Phase 2.3.
 * Chuyen biet ve: dong tien, AOV, ROI khuyen mai, phan tich theo kenh ban.
 *
 * Su dung boi FinanceAgent (Phase 2.3).
 * Phan biet voi AdminReportTools: tools nay tap trung vao TINH TOAN TAI CHINH.
 */
@Slf4j
@Component("adminFinanceTools")
@RequiredArgsConstructor
public class AdminFinanceTools {

    private final ReportFeignClient reportFeignClient;

    @Tool("Phân tích ROI (Hiệu quả đầu tư) của từng chương trình khuyến mãi. " +
          "Tính toán: số tiền giảm giá bỏ ra (Chi phí), doanh thu tạo ra (Lợi ích), tỷ lệ ROI. " +
          "Dùng khi admin hỏi: 'KM nào hiệu quả nhất', 'khuyến mãi có lời không', 'nên giữ KM nào'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getPromotionROIAnalysis(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                          @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getPromotionROIAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getPromotionEffectiveness(fromDate, toDate, 100);
            if (res == null || res.getData() == null || res.getData().getContent() == null || res.getData().getContent().isEmpty()) {
                return "Không có dữ liệu khuyến mãi trong khoảng " + from + " đến " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("💹 PHÂN TÍCH ROI KHUYẾN MÃI từ ").append(from).append(" đến ").append(to).append(":\n\n");

            // Tinh tong
            BigDecimal totalRevenue  = BigDecimal.ZERO;
            BigDecimal totalDiscount = BigDecimal.ZERO;
            long totalOrders = 0;

            for (var promo : res.getData().getContent()) {
                totalRevenue  = totalRevenue.add(promo.grossRevenue()  != null ? promo.grossRevenue()  : BigDecimal.ZERO);
                totalDiscount = totalDiscount.add(promo.totalDiscountGiven() != null ? promo.totalDiscountGiven() : BigDecimal.ZERO);
                totalOrders  += promo.orderCount();

                // ROI = (Doanh thu - Chi phi giam gia) / Chi phi giam gia * 100%
                BigDecimal discount = promo.totalDiscountGiven() != null ? promo.totalDiscountGiven() : BigDecimal.ZERO;
                String roiStr;
                if (discount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal netRevenue = promo.grossRevenue().subtract(discount);
                    BigDecimal roi = netRevenue.divide(discount, 4, RoundingMode.HALF_UP)
                                               .multiply(BigDecimal.valueOf(100))
                                               .setScale(1, RoundingMode.HALF_UP);
                    roiStr = (roi.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + roi + "%";
                } else {
                    roiStr = "N/A (không giảm giá)";
                }

                sb.append("• [").append(promo.promotionCode()).append("]\n")
                  .append("  Doanh thu: ").append(formatVnd(promo.grossRevenue()))
                  .append(" | Chi giảm: ").append(formatVnd(discount))
                  .append(" | Số đơn: ").append(promo.orderCount())
                  .append(" | AOV: ").append(formatVnd(promo.avgOrderValue()))
                  .append(" | ROI: ").append(roiStr).append("\n");
            }

            sb.append("\n📊 TỔNG KẾT:\n");
            sb.append("  Tổng doanh thu từ KM: ").append(formatVnd(totalRevenue)).append("\n");
            sb.append("  Tổng tiền giảm: ").append(formatVnd(totalDiscount)).append("\n");
            sb.append("  Tổng đơn có KM: ").append(totalOrders).append("\n");

            // Khuyen nghi
            if (totalDiscount.compareTo(totalRevenue.multiply(BigDecimal.valueOf(0.3))) > 0) {
                sb.append("\n⚠️ CẢNH BÁO: Chi phí giảm giá vượt 30% doanh thu KM. ")
                  .append("Nên xem xét lại điều kiện áp dụng hoặc giá trị giảm.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getPromotionROIAnalysis error: {}", e.getMessage());
            return "Lỗi khi phân tích ROI khuyến mãi. Vui lòng thử lại sau.";
        }
    }

    @Tool("Phân tích xu hướng AOV (Giá trị đơn hàng trung bình) theo thời gian. " +
          "Tính toán: AOV theo ngày, xu hướng tăng/giảm, ngày có AOV cao nhất/thấp nhất. " +
          "Dùng khi admin hỏi: 'AOV đang thế nào', 'khách chi tiêu bao nhiêu', 'giá trị đơn hàng có tăng không'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getAovTrendAnalysis(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                      @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getAovTrendAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueReport(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu doanh thu trong khoảng " + from + " đến " + to + ".";
            }

            List<ReportFeignClient.RevenueRow> rows = res.getData();
            BigDecimal totalRevenue = rows.stream().map(ReportFeignClient.RevenueRow::revenue)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long totalOrders = rows.stream().mapToLong(ReportFeignClient.RevenueRow::totalOrders).sum();
            BigDecimal overallAov = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

            // Tim ngay AOV cao nhat / thap nhat
            var maxDay = rows.stream()
                    .filter(r -> r.totalOrders() > 0)
                    .max(Comparator.comparing(r ->
                            r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)));
            var minDay = rows.stream()
                    .filter(r -> r.totalOrders() > 0)
                    .min(Comparator.comparing(r ->
                            r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)));

            StringBuilder sb = new StringBuilder();
            sb.append("💳 PHÂN TÍCH AOV từ ").append(from).append(" đến ").append(to).append(":\n\n");
            sb.append("AOV tổng thể: ").append(formatVnd(overallAov)).append("/đơn\n");
            sb.append("Tổng doanh thu: ").append(formatVnd(totalRevenue))
              .append(" / ").append(totalOrders).append(" đơn\n\n");

            // Chi tiet theo ngay
            sb.append("Chi tiết theo ngày:\n");
            rows.forEach(r -> {
                BigDecimal dayAov = r.totalOrders() > 0
                        ? r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("• ").append(r.day())
                  .append(": AOV=").append(formatVnd(dayAov))
                  .append(" (").append(r.totalOrders()).append(" đơn, ").append(formatVnd(r.revenue())).append(")\n");
            });

            maxDay.ifPresent(r -> {
                BigDecimal aov = r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP);
                sb.append("\n🏆 Ngày AOV cao nhất: ").append(r.day()).append(" — ").append(formatVnd(aov));
            });
            minDay.ifPresent(r -> {
                BigDecimal aov = r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP);
                sb.append("\n📉 Ngày AOV thấp nhất: ").append(r.day()).append(" — ").append(formatVnd(aov));
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getAovTrendAnalysis error: {}", e.getMessage());
            return "Lỗi khi phân tích AOV. Vui lòng thử lại sau.";
        }
    }

    @Tool("Phân tích cơ cấu doanh thu theo kênh bán (QR/MANUAL) và xu hướng. " +
          "Xác định kênh nào đang tăng trưởng, kênh nào cần đẩy mạnh. " +
          "Dùng khi admin hỏi: 'kênh nào hiệu quả', 'QR chiếm bao nhiêu', 'nên đầu tư kênh nào'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getRevenueChannelAnalysis(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                            @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getRevenueChannelAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueBySource(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu theo kênh bán trong khoảng " + from + " đến " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📡 PHÂN TÍCH KÊNH BÁN từ ").append(from).append(" đến ").append(to).append(":\n\n");

            res.getData().forEach(row -> {
                BigDecimal aovChannel = row.totalOrders() > 0
                        ? row.revenue().divide(BigDecimal.valueOf(row.totalOrders()), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("• ").append(row.source()).append(":\n")
                  .append("  Doanh thu: ").append(formatVnd(row.revenue()))
                  .append(" (").append(row.percentage()).append("% tổng)\n")
                  .append("  Số đơn: ").append(row.totalOrders())
                  .append(" | AOV: ").append(formatVnd(aovChannel)).append("\n");
            });

            // Khuyen nghi chien luoc kenh
            sb.append("\n💡 NHẬN ĐỊNH KÊNH:\n");
            var dominantSource = res.getData().stream()
                    .max(Comparator.comparingDouble(ReportFeignClient.SourceRow::percentage));
            dominantSource.ifPresent(src -> {
                sb.append("• Kênh chủ đạo: ").append(src.source()).append(" (").append(src.percentage()).append("%)\n");
                if (src.percentage() > 80) {
                    sb.append("• ⚠️ Quá phụ thuộc vào 1 kênh. Cần đa dạng hóa để giảm rủi ro.\n");
                }
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getRevenueChannelAnalysis error: {}", e.getMessage());
            return "Lỗi khi phân tích kênh bán. Vui lòng thử lại sau.";
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }
}
