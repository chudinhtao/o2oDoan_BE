package com.fnb.ai.tools;

import com.fnb.ai.feign.KdsFeignClient;
import com.fnb.ai.feign.ReportFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("adminKdsTools")
@RequiredArgsConstructor
public class AdminKdsTools {

    private final KdsFeignClient kdsFeignClient;
    private final ReportFeignClient reportFeignClient;

    @Tool("Kiểm tra tình trạng bếp hiện tại (Active Kitchen Tickets). " +
          "Dùng khi admin hỏi: 'bếp đang có bao nhiêu đơn', 'bếp có đang quá tải không'.")
    public String getActiveKitchenTickets() {
        log.info("[KDS-TOOL] getActiveKitchenTickets");
        try {
            List<Map<String, Object>> tickets = kdsFeignClient.getActiveTickets(null);
            if (tickets == null || tickets.isEmpty()) {
                return "✅ Hiện tại bếp đang rảnh, không có đơn hàng nào đang chờ.";
            }

            int pendingCount = 0;
            int preparingCount = 0;
            int totalItems = 0;

            for (Map<String, Object> ticket : tickets) {
                String status = (String) ticket.get("status");
                if ("PENDING".equalsIgnoreCase(status)) pendingCount++;
                if ("PREPARING".equalsIgnoreCase(status)) preparingCount++;

                Object itemsObj = ticket.get("items");
                if (itemsObj instanceof List<?> items) {
                    for (Object itemObj : items) {
                        if (itemObj instanceof Map<?, ?> item) {
                            String itemStatus = (String) item.get("status");
                            if ("PENDING".equalsIgnoreCase(itemStatus) || "PREPARING".equalsIgnoreCase(itemStatus)) {
                                totalItems++;
                            }
                        }
                    }
                }
            }

            if (pendingCount == 0 && preparingCount == 0) {
                return "✅ Các đơn gần nhất đều đã làm xong. Bếp hiện đang rảnh.";
            }

            StringBuilder sb = new StringBuilder("🍳 TÌNH TRẠNG BẾP HIỆN TẠI:\n\n");
            sb.append("• Vé đang chờ (PENDING): ").append(pendingCount).append(" vé\n");
            sb.append("• Vé đang chế biến (PREPARING): ").append(preparingCount).append(" vé\n");
            sb.append("• Tổng số món ăn đang phải làm: ").append(totalItems).append(" món\n\n");
            
            if (pendingCount + preparingCount > 10) {
                sb.append("⚠️ Bếp hiện đang khá đông! Có thể xảy ra tình trạng quá tải.");
            } else {
                sb.append("✅ Nhịp độ bếp đang ổn định, nằm trong tầm kiểm soát.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[KDS-TOOL] getActiveKitchenTickets error: {}", e.getMessage());
            return "Lỗi khi kết nối hệ thống KDS. Vui lòng thử lại.";
        }
    }

    @Tool("Kiểm tra và phát hiện các món ăn bị chậm trễ, ùn tắc tại bếp (Kitchen Bottlenecks). " +
          "Dùng khi admin hỏi: 'có món nào bị chậm không', 'trạm bếp nào đang bị kẹt'. " +
          "Tiêu chí chậm: Món ăn nằm ở PENDING hoặc PREPARING quá 15 phút.")
    public String getKitchenBottlenecks() {
        log.info("[KDS-TOOL] getKitchenBottlenecks");
        try {
            List<Map<String, Object>> tickets = kdsFeignClient.getActiveTickets(LocalDateTime.now().minusHours(3));
            if (tickets == null || tickets.isEmpty()) {
                return "✅ Bếp hiện tại đang hoạt động tốt, không có món ăn nào bị ùn tắc.";
            }

            LocalDateTime now = LocalDateTime.now();
            StringBuilder sb = new StringBuilder("⏳ PHÂN TÍCH ÙN TẮC BẾP (Chờ > 15 phút):\n\n");
            boolean hasBottleneck = false;

            for (Map<String, Object> ticket : tickets) {
                String ticketStatus = (String) ticket.get("status");
                if ("PENDING".equalsIgnoreCase(ticketStatus) || "PREPARING".equalsIgnoreCase(ticketStatus)) {
                    
                    String createdAtStr = (String) ticket.get("createdAt");
                    if (createdAtStr != null) {
                        LocalDateTime createdAt = LocalDateTime.parse(createdAtStr, DateTimeFormatter.ISO_DATE_TIME);
                        long waitingMinutes = Duration.between(createdAt, now).toMinutes();

                        if (waitingMinutes >= 15) {
                            hasBottleneck = true;
                            Object tableNo = ticket.get("tableNumber");
                            sb.append("   - Bàn ").append(tableNo != null ? tableNo : "Mang đi")
                              .append(" - Chờ ").append(waitingMinutes).append(" phút (").append(ticketStatus).append(")\n");
                            
                            // Liet ke cac mon chua xong
                            Object itemsObj = ticket.get("items");
                            if (itemsObj instanceof List<?> items) {
                                for (Object itemObj : items) {
                                    if (itemObj instanceof Map<?, ?> item) {
                                        String itemStatus = (String) item.get("status");
                                        if ("PENDING".equalsIgnoreCase(itemStatus) || "PREPARING".equalsIgnoreCase(itemStatus)) {
                                            sb.append("   - ").append(item.get("itemName"))
                                              .append(" (Trạm: ").append(item.get("station") != null ? item.get("station") : "Chung").append(")\n");
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!hasBottleneck) {
                return "✅ Tuyệt vời! Không có đơn hàng nào bị trễ quá 15 phút. Hiệu suất bếp rất tốt.";
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[KDS-TOOL] getKitchenBottlenecks error: {}", e.getMessage());
            return "Lỗi khi phân tích ùn tắc bếp.";
        }
    }

    @Tool("Thống kê hiệu suất chế biến của Bếp (Kitchen Performance Summary). " +
          "Dùng khi admin hỏi: 'thời gian làm món trung bình', 'hiệu suất bếp hôm nay', 'tỷ lệ trễ đơn'. " +
          "Tham số from/to là datetime yyyy-MM-dd")
    public String getKitchenPerformanceSummary(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                               @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[KDS-TOOL] getKitchenPerformanceSummary from={} to={}", from, to);
        try {
            LocalDate startDate = LocalDate.parse(from);
            LocalDate endDate   = LocalDate.parse(to);
            var res = reportFeignClient.getKitchenPerformance(startDate, endDate, 1000);
            
            if (res == null || res.getData() == null || res.getData().getContent() == null || res.getData().getContent().isEmpty()) {
                return "Không có dữ liệu đánh giá hiệu suất bếp trong thời gian này.";
            }

            var rows = res.getData().getContent();
            StringBuilder sb = new StringBuilder("📈 HIỆU SUẤT BẾP (Từ ").append(from).append(" đến ").append(to).append("):\n\n");
            
            long totalTickets = 0;
            long totalLate = 0;
            double avgPrepTimeSum = 0;

            for (var r : rows) {
                totalTickets += r.totalTickets();
                totalLate += r.lateTickets();
                avgPrepTimeSum += r.avgPrepMinutes().doubleValue() * r.totalTickets();
                
                // Show top 3 worst items? Just listing them is fine for now
                if (r.lateRate() > 20.0) { // Cảnh báo các món trễ trên 20%
                    sb.append("⚠️ ").append(r.itemName())
                      .append(" (T/g TB: ").append(r.avgPrepMinutes()).append("p | Trễ: ").append(String.format("%.1f%%", r.lateRate())).append(")\n");
                }
            }
            
            double overallAvg = totalTickets > 0 ? (avgPrepTimeSum / totalTickets) : 0;
            double overallLateRate = totalTickets > 0 ? (totalLate * 100.0 / totalTickets) : 0;

            sb.append("\nTổng số vé (tickets): ").append(totalTickets).append("\n");
            sb.append("Thời gian chế biến trung bình: ").append(String.format("%.1f", overallAvg)).append(" phút/món\n");
            sb.append("Tỷ lệ đơn trễ: ").append(String.format("%.1f%%", overallLateRate)).append("\n");
            
            if (overallLateRate > 15.0) {
                sb.append("\n❌ Tỷ lệ đơn trễ đang mức CAO. Cần xem lại quy trình chuẩn bị hoặc tăng cường nhân sự.");
            } else {
                sb.append("\n✅ Hiệu suất ổn định. Bếp đang hoạt động tốt.");
            }

            return sb.toString();
        } catch (Exception e) {
            log.error("[KDS-TOOL] getKitchenPerformanceSummary error: {}", e.getMessage());
            return "Lỗi khi tính toán hiệu suất bếp.";
        }
    }
}
