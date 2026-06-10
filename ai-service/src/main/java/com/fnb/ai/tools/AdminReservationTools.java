package com.fnb.ai.tools;

import com.fnb.ai.feign.ReservationFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component("adminReservationTools")
@RequiredArgsConstructor
public class AdminReservationTools {

    private final ReservationFeignClient reservationFeignClient;

    @Tool("Kiểm tra sức chứa nhà hàng có đủ bàn trong tương lai hay không (Check Capacity). " +
          "Dùng khi admin/lễ tân hỏi: 'tối mai lúc 19h còn nhận được bàn 10 người không'. " +
          "Tham số time là datetime yyyy-MM-dd'T'HH:mm:ss, partySize là số người.")
    public String checkTableCapacity(@P("Thời gian muốn kiểm tra (yyyy-MM-dd'T'HH:mm:ss)") String time,
                                     @P("Số lượng khách (partySize)") int partySize) {
        log.info("[RESERVATION-TOOL] checkTableCapacity time={} partySize={}", time, partySize);
        try {
            LocalDateTime checkTime = LocalDateTime.parse(time);
            var res = reservationFeignClient.checkCapacity(checkTime, partySize);
            if (res != null && Boolean.TRUE.equals(res.getData())) {
                return "✅ Nhà hàng CÓ ĐỦ sức chứa cho đoàn " + partySize + " người vào lúc " + time + ". Có thể nhận khách.";
            } else {
                return "❌ Nhà hàng ĐÃ KÍN BÀN hoặc không đủ sức chứa cho đoàn " + partySize + " người vào lúc " + time + ".";
            }
        } catch (Exception e) {
            log.error("[RESERVATION-TOOL] checkTableCapacity error: {}", e.getMessage());
            return "Lỗi khi kiểm tra sức chứa hệ thống.";
        }
    }

    @Tool("Lấy danh sách khách đặt bàn (Reservations) trong 1 khoảng thời gian. " +
          "Dùng để xem lịch đặt bàn hôm nay hoặc tương lai. " +
          "Tham số from/to là datetime yyyy-MM-dd'T'HH:mm:ss")
    public String getUpcomingReservations(@P("Ngày bắt đầu (yyyy-MM-dd'T'HH:mm:ss)") String from,
                                          @P("Ngày kết thúc (yyyy-MM-dd'T'HH:mm:ss)") String to) {
        log.info("[RESERVATION-TOOL] getUpcomingReservations from={} to={}", from, to);
        try {
            LocalDateTime startDate = LocalDateTime.parse(from);
            LocalDateTime endDate   = LocalDateTime.parse(to);
            var res = reservationFeignClient.getAdminReservations(null, null, startDate, endDate, 0, 50);
            
            if (res == null || res.getData() == null || res.getData().content().isEmpty()) {
                return "Không có khách đặt bàn nào trong khoảng thời gian từ " + from + " đến " + to + ".";
            }

            var rows = res.getData().content();
            StringBuilder sb = new StringBuilder("📅 DANH SÁCH KHÁCH ĐẶT BÀN (Từ ").append(from).append(" - ").append(to).append("):\n\n");
            
            int totalGuests = 0;
            for (var r : rows) {
                totalGuests += (r.partySize() != null ? r.partySize() : 0);
                sb.append("• ").append(r.bookingTime()).append(" - Khách: ").append(r.customerName())
                  .append(" (").append(r.partySize()).append(" người)")
                  .append("\n  SDT: ").append(r.customerPhone())
                  .append(" | Trạng thái: ").append(r.status())
                  .append(" | Cọc: ").append(formatVnd(r.depositAmount()));
                if (r.note() != null && !r.note().isEmpty()) {
                    sb.append(" | Ghi chú: ").append(r.note());
                }
                sb.append("\n");
            }
            sb.append("\nTổng cộng: ").append(rows.size()).append(" bàn / ").append(totalGuests).append(" khách.");
            return sb.toString();
        } catch (Exception e) {
            log.error("[RESERVATION-TOOL] getUpcomingReservations error: {}", e.getMessage());
            return "Lỗi khi lấy danh sách đặt bàn.";
        }
    }

    @Tool("Thống kê nhanh tỷ lệ khách hủy, khách đến và đang chờ của các bàn đã đặt (Reservation Status Summary). " +
          "Dùng khi admin hỏi 'tỷ lệ hủy bàn', 'bao nhiêu khách không đến'. " +
          "Tham số from/to là datetime yyyy-MM-dd'T'HH:mm:ss")
    public String getReservationStatusSummary(@P("Ngày bắt đầu (yyyy-MM-dd'T'HH:mm:ss)") String from,
                                              @P("Ngày kết thúc (yyyy-MM-dd'T'HH:mm:ss)") String to) {
        log.info("[RESERVATION-TOOL] getReservationStatusSummary from={} to={}", from, to);
        try {
            LocalDateTime startDate = LocalDateTime.parse(from);
            LocalDateTime endDate   = LocalDateTime.parse(to);
            // Lay toi da de thong ke memory (vi API nay ho tro page)
            var res = reservationFeignClient.getAdminReservations(null, null, startDate, endDate, 0, 1000);
            
            if (res == null || res.getData() == null || res.getData().content().isEmpty()) {
                return "Không có dữ liệu thống kê đặt bàn.";
            }

            long total = res.getData().content().size();
            long confirmed = res.getData().content().stream().filter(r -> "CONFIRMED".equals(r.status()) || "COMPLETED".equals(r.status())).count();
            long cancelled = res.getData().content().stream().filter(r -> "CANCELLED".equals(r.status())).count();
            long pending = total - confirmed - cancelled;

            StringBuilder sb = new StringBuilder("📊 THỐNG KÊ TRẠNG THÁI ĐẶT BÀN từ ").append(from).append(" đến ").append(to).append(":\n\n");
            sb.append("Tổng số lượt đặt: ").append(total).append("\n");
            sb.append("✅ Khách đã đến / Xác nhận: ").append(confirmed).append(" (").append(String.format("%.1f%%", (confirmed * 100.0) / total)).append(")\n");
            sb.append("❌ Khách hủy bàn (Cancelled): ").append(cancelled).append(" (").append(String.format("%.1f%%", (cancelled * 100.0) / total)).append(")\n");
            sb.append("⏳ Đang chờ phục vụ (Pending): ").append(pending).append(" (").append(String.format("%.1f%%", (pending * 100.0) / total)).append(")\n");

            return sb.toString();
        } catch (Exception e) {
            log.error("[RESERVATION-TOOL] getReservationStatusSummary error: {}", e.getMessage());
            return "Lỗi khi tính toán thống kê đặt bàn.";
        }
    }

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }
}
