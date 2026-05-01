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

    @Tool("Phan tich ROI (Hieu qua dau tu) cua tung chuong trinh khuyen mai. " +
          "Tinh toan: so tien giam gia bo ra (Chi phi), doanh thu tao ra (Loi ich), ty le ROI. " +
          "Dung khi admin hoi: 'KM nao hieu qua nhat', 'khuyen mai co loi khong', 'nen giu KM nao'. " +
          "Tham so from/to dinh dang yyyy-MM-dd.")
    public String getPromotionROIAnalysis(@P("Ngay bat dau (yyyy-MM-dd)") String from,
                                          @P("Ngay ket thuc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getPromotionROIAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getPromotionEffectiveness(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Khong co du lieu khuyen mai trong khoang " + from + " den " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("💹 PHAN TICH ROI KHUYEN MAI tu ").append(from).append(" den ").append(to).append(":\n\n");

            // Tinh tong
            BigDecimal totalRevenue  = BigDecimal.ZERO;
            BigDecimal totalDiscount = BigDecimal.ZERO;
            long totalOrders = 0;

            for (var promo : res.getData()) {
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
                    roiStr = "N/A (khong giam gia)";
                }

                sb.append("• [").append(promo.promotionCode()).append("]\n")
                  .append("  Doanh thu: ").append(formatVnd(promo.grossRevenue()))
                  .append(" | Chi giam: ").append(formatVnd(discount))
                  .append(" | So don: ").append(promo.orderCount())
                  .append(" | AOV: ").append(formatVnd(promo.avgOrderValue()))
                  .append(" | ROI: ").append(roiStr).append("\n");
            }

            sb.append("\n📊 TONG KET:\n");
            sb.append("  Tong doanh thu tu KM: ").append(formatVnd(totalRevenue)).append("\n");
            sb.append("  Tong tien giam: ").append(formatVnd(totalDiscount)).append("\n");
            sb.append("  Tong don co KM: ").append(totalOrders).append("\n");

            // Khuyen nghi
            if (totalDiscount.compareTo(totalRevenue.multiply(BigDecimal.valueOf(0.3))) > 0) {
                sb.append("\n⚠️ CANH BAO: Chi phi giam gia vuot 30% doanh thu KM. ")
                  .append("Nen xem xet lai dieu kien ap dung hoac gia tri giam.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getPromotionROIAnalysis error: {}", e.getMessage());
            return "Loi khi phan tich ROI khuyen mai. Vui long thu lai sau.";
        }
    }

    @Tool("Phan tich xu huong AOV (Gia tri don hang trung binh) theo thoi gian. " +
          "Tinh toan: AOV theo ngay, xu huong tang/giam, ngay co AOV cao nhat/thap nhat. " +
          "Dung khi admin hoi: 'AOV dang the nao', 'khach chi tieu bao nhieu', 'gia tri don hang co tang khong'. " +
          "Tham so from/to dinh dang yyyy-MM-dd.")
    public String getAovTrendAnalysis(@P("Ngay bat dau (yyyy-MM-dd)") String from,
                                      @P("Ngay ket thuc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getAovTrendAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueReport(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Khong co du lieu doanh thu trong khoang " + from + " den " + to + ".";
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
                    .max(java.util.Comparator.comparing(r ->
                            r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)));
            var minDay = rows.stream()
                    .filter(r -> r.totalOrders() > 0)
                    .min(java.util.Comparator.comparing(r ->
                            r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)));

            StringBuilder sb = new StringBuilder();
            sb.append("💳 PHAN TICH AOV tu ").append(from).append(" den ").append(to).append(":\n\n");
            sb.append("AOV tong the: ").append(formatVnd(overallAov)).append("/don\n");
            sb.append("Tong doanh thu: ").append(formatVnd(totalRevenue))
              .append(" / ").append(totalOrders).append(" don\n\n");

            // Xu huong theo ngay (top 7)
            sb.append("Chi tiet theo ngay:\n");
            rows.forEach(r -> {
                BigDecimal dayAov = r.totalOrders() > 0
                        ? r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("• ").append(r.day())
                  .append(": AOV=").append(formatVnd(dayAov))
                  .append(" (").append(r.totalOrders()).append(" don, ").append(formatVnd(r.revenue())).append(")\n");
            });

            maxDay.ifPresent(r -> {
                BigDecimal aov = r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP);
                sb.append("\n🏆 Ngay AOV cao nhat: ").append(r.day()).append(" — ").append(formatVnd(aov));
            });
            minDay.ifPresent(r -> {
                BigDecimal aov = r.revenue().divide(BigDecimal.valueOf(r.totalOrders()), 0, RoundingMode.HALF_UP);
                sb.append("\n📉 Ngay AOV thap nhat: ").append(r.day()).append(" — ").append(formatVnd(aov));
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getAovTrendAnalysis error: {}", e.getMessage());
            return "Loi khi phan tich AOV. Vui long thu lai sau.";
        }
    }

    @Tool("Phan tich co cau doanh thu theo kenh ban (QR/MANUAL) va xu huong. " +
          "Xac dinh kenh nao dang tang truong, kenh nao can day manh. " +
          "Dung khi admin hoi: 'kenh nao hieu qua', 'QR chiem bao nhieu', 'nen dau tu kenh nao'. " +
          "Tham so from/to dinh dang yyyy-MM-dd.")
    public String getRevenueChannelAnalysis(@P("Ngay bat dau (yyyy-MM-dd)") String from,
                                            @P("Ngay ket thuc (yyyy-MM-dd)") String to) {
        log.info("[FINANCE-TOOL] getRevenueChannelAnalysis from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);
            var res = reportFeignClient.getRevenueBySource(fromDate, toDate);
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Khong co du lieu theo kenh ban trong khoang " + from + " den " + to + ".";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📡 PHAN TICH KENH BAN tu ").append(from).append(" den ").append(to).append(":\n\n");

            res.getData().forEach(row -> {
                BigDecimal aovChannel = row.totalOrders() > 0
                        ? row.revenue().divide(BigDecimal.valueOf(row.totalOrders()), 0, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                sb.append("• ").append(row.source()).append(":\n")
                  .append("  Doanh thu: ").append(formatVnd(row.revenue()))
                  .append(" (").append(row.percentage()).append("% tong)\n")
                  .append("  So don: ").append(row.totalOrders())
                  .append(" | AOV: ").append(formatVnd(aovChannel)).append("\n");
            });

            // Khuyen nghi chien luoc kenh
            sb.append("\n💡 NHAN DINH KENH:\n");
            var dominantSource = res.getData().stream()
                    .max(java.util.Comparator.comparingDouble(ReportFeignClient.SourceRow::percentage));
            dominantSource.ifPresent(src -> {
                sb.append("• Kenh chu dao: ").append(src.source()).append(" (").append(src.percentage()).append("%)\n");
                if (src.percentage() > 80) {
                    sb.append("• ⚠️ Qua phu thuoc vao 1 kenh. Can da dang hoa de giam rui ro.\n");
                }
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("[FINANCE-TOOL] getRevenueChannelAnalysis error: {}", e.getMessage());
            return "Loi khi phan tich kenh ban. Vui long thu lai sau.";
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0d";
        return String.format("%,.0f", amount) + "d";
    }
}
