package com.fnb.ai.tools;

import com.fnb.ai.feign.OrderFeignClient;
import com.fnb.ai.feign.ReportFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component("adminPosTools")
@RequiredArgsConstructor
public class AdminPosTools {

    private final OrderFeignClient orderFeignClient;
    private final ReportFeignClient reportFeignClient;

    @Tool("Kiểm tra tình trạng bàn đang hoạt động và tổng tiền tạm tính (Live POS Status). " +
          "Dùng khi admin hỏi: 'hiện đang có bao nhiêu bàn', 'quán có đông không', 'tổng tiền đang ăn là bao nhiêu'.")
    public String getLivePosStatus() {
        log.info("[POS-TOOL] getLivePosStatus");
        try {
            var res = orderFeignClient.getAllTablesForPos();
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu bàn từ hệ thống POS.";
            }

            List<OrderFeignClient.PosTableRow> tables = res.getData();
            long activeTables = tables.stream().filter(t -> "OCCUPIED".equalsIgnoreCase(t.status())).count();
            long freeTables = tables.stream().filter(t -> "FREE".equalsIgnoreCase(t.status())).count();
            
            BigDecimal totalUnsettled = tables.stream()
                .filter(t -> "OCCUPIED".equalsIgnoreCase(t.status()) && t.totalAmount() != null)
                .map(OrderFeignClient.PosTableRow::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            StringBuilder sb = new StringBuilder("🏪 TÌNH TRẠNG QUÁN HIỆN TẠI (Live POS):\n\n");
            sb.append("• Bàn đang có khách (OCCUPIED): ").append(activeTables).append("\n");
            sb.append("• Bàn trống (FREE): ").append(freeTables).append("\n");
            sb.append("• Tổng tiền đang phục vụ (Chưa thanh toán): ").append(formatVnd(totalUnsettled)).append("\n\n");

            if (activeTables > 0) {
                sb.append("Danh sách bàn đang hoạt động:\n");
                tables.stream().filter(t -> "OCCUPIED".equalsIgnoreCase(t.status())).forEach(t -> {
                    sb.append("   - Bàn ").append(t.number() != null ? t.number() : t.name())
                      .append(" (").append(t.zone() != null ? t.zone() : "Chung").append(")");
                    if (t.totalAmount() != null && t.totalAmount().compareTo(BigDecimal.ZERO) > 0) {
                        sb.append(" | Tạm tính: ").append(formatVnd(t.totalAmount()));
                    }
                    sb.append("\n");
                });
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("[POS-TOOL] getLivePosStatus error: {}", e.getMessage());
            return "Lỗi khi truy xuất hệ thống POS. Vui lòng thử lại.";
        }
    }

    @Tool("Thống kê tỷ trọng các phương thức thanh toán (Tiền mặt, Thẻ, Chuyển khoản). " +
          "Dùng khi admin hỏi: 'khách thường trả bằng gì', 'hôm nay thu tiền mặt bao nhiêu'.")
    public String getPaymentMethodsSummary(@P("Ngày cần kiểm tra (yyyy-MM-dd)") String date) {
        log.info("[POS-TOOL] getPaymentMethodsSummary date={}", date);
        try {
            var res = reportFeignClient.getCashierShiftReport(LocalDate.parse(date));
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu báo cáo ca làm việc ngày " + date;
            }

            var data = res.getData();
            if (data.revenueByPaymentMethod() == null || data.revenueByPaymentMethod().isEmpty()) {
                return "Chưa có giao dịch thanh toán nào được ghi nhận trong ngày " + date;
            }

            StringBuilder sb = new StringBuilder("💳 THỐNG KÊ PHƯƠNG THỨC THANH TOÁN (").append(date).append("):\n\n");
            BigDecimal totalRev = data.totalRevenue() != null ? data.totalRevenue() : BigDecimal.ZERO;
            
            data.revenueByPaymentMethod().forEach((method, amount) -> {
                long orderCount = 0;
                if (data.ordersByPaymentMethod() != null && data.ordersByPaymentMethod().containsKey(method)) {
                    orderCount = data.ordersByPaymentMethod().get(method);
                }
                
                double pct = totalRev.compareTo(BigDecimal.ZERO) > 0 
                             ? amount.multiply(new BigDecimal("100")).divide(totalRev, 1, java.math.RoundingMode.HALF_UP).doubleValue() 
                             : 0;
                
                sb.append("• ").append(method).append(": ")
                  .append(formatVnd(amount)).append(" (").append(pct).append("%) - ")
                  .append(orderCount).append(" đơn\n");
            });

            return sb.toString();
        } catch (Exception e) {
            log.error("[POS-TOOL] getPaymentMethodsSummary error: {}", e.getMessage());
            return "Lỗi khi tổng hợp phương thức thanh toán.";
        }
    }

    @Tool("Kiểm tra và phân tích các giao dịch bị hủy (Void) hoặc giảm giá bất thường. " +
          "Dùng để chống gian lận hoặc kiểm tra lý do doanh thu hao hụt. " +
          "Tham số from/to định dạng yyyy-MM-dd")
    public String getVoidAndDiscountAlerts(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                           @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[POS-TOOL] getVoidAndDiscountAlerts from={} to={}", from, to);
        try {
            LocalDate startDate = LocalDate.parse(from);
            LocalDate endDate = LocalDate.parse(to);
            var res = reportFeignClient.getCancelledOrderDrilldown(startDate, endDate, 1000);
            
            if (res == null || res.getData() == null || res.getData().getContent() == null || res.getData().getContent().isEmpty()) {
                return "✅ Rất tốt! Không có giao dịch hủy đơn (Void) nào trong khoảng thời gian này.";
            }

            StringBuilder sb = new StringBuilder("🚫 CẢNH BÁO ĐƠN HỦY / VOID (Từ ").append(from).append(" đến ").append(to).append("):\n\n");
            
            long totalVoids = 0;
            BigDecimal totalLostRevenue = BigDecimal.ZERO;

            for (var row : res.getData().getContent()) {
                totalVoids += row.cancelCount();
                if (row.cancelledRevenue() != null) {
                    totalLostRevenue = totalLostRevenue.add(row.cancelledRevenue());
                }
                sb.append("• Lý do: ").append(row.cancellationReason() != null ? row.cancellationReason() : "Khác")
                  .append(" - ").append(row.cancelCount()).append(" đơn (Thất thoát khoảng ")
                  .append(formatVnd(row.cancelledRevenue())).append(")\n");
            }
            
            sb.append("\nTổng số đơn hủy: ").append(totalVoids).append("\n");
            sb.append("Tổng doanh thu ước tính thất thoát: ").append(formatVnd(totalLostRevenue)).append("\n");

            if (totalVoids > 5 || totalLostRevenue.compareTo(new BigDecimal("1000000")) > 0) {
                sb.append("\n⚠️ LƯU Ý: Tỷ lệ hủy/Void đang khá cao. Cần tra soát lại camera thu ngân hoặc kiểm tra nguồn nguyên liệu (nếu lý do là hết hàng).");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("[POS-TOOL] getVoidAndDiscountAlerts error: {}", e.getMessage());
            return "Lỗi khi phân tích dữ liệu Void/Hủy.";
        }
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }
}
