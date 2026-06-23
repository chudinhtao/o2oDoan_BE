package com.fnb.ai.tools;

import com.fnb.ai.feign.MenuFeignClient;
import com.fnb.ai.feign.ReportFeignClient;
import com.fnb.ai.feign.StaffFeignClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Bo cong cu Van Hanh cho Admin AI — Phase 1.5 + 2.4.
 * Cung cap:
 *   - getStaffWorkload      : So luong nhan vien theo vai tro + tai trong
 *   - getMenuOverview       : Trang thai menu (het hang, dang sale, theo station)
 *   - getOperationalSummary : Tong hop van hanh (bep + nhan vien + menu) trong 1 tool
 *
 * Tat ca READ-ONLY, khong co side effect.
 */
@Slf4j
@Component("adminOperationalTools")
@RequiredArgsConstructor
public class AdminOperationalTools {

    private final StaffFeignClient staffFeignClient;
    private final MenuFeignClient menuFeignClient;
    private final ReportFeignClient reportFeignClient;
    private final com.fnb.ai.feign.OrderFeignClient orderFeignClient;

    @Tool("Lấy danh sách nhân viên và phân tích tải trọng: số lượng theo vai trò (CASHIER/KITCHEN/ADMIN), " +
          "ai đang hoạt động, ai bị khóa. " +
          "Dùng khi admin hỏi: 'bao nhiêu nhân viên', 'staff hiện tại', 'kiểm tra nhân sự', 'thiếu người không'.")
    public String getStaffWorkload() {
        log.info("[OPS-TOOL] getStaffWorkload");
        try {
            var res = staffFeignClient.getAllStaff();
            if (res == null || res.getData() == null || res.getData().getContent().isEmpty()) {
                return "Không có dữ liệu nhân viên.";
            }

            List<StaffFeignClient.StaffRow> staff = res.getData().getContent();
            long total  = staff.size();
            long active = staff.stream().filter(StaffFeignClient.StaffRow::active).count();
            long inactive = total - active;

            // Phan loai theo role
            Map<String, Long> byRole = staff.stream()
                    .filter(StaffFeignClient.StaffRow::active)
                    .collect(Collectors.groupingBy(
                            s -> s.role() != null ? s.role() : "UNKNOWN",
                            Collectors.counting()
                    ));

            StringBuilder sb = new StringBuilder();
            sb.append("👥 NHÂN SỰ HIỆN TẠI:\n\n");
            sb.append("Tổng số: ").append(total)
              .append(" nhân viên | Đang hoạt động: ").append(active)
              .append(" | Bị khóa: ").append(inactive).append("\n\n");
            sb.append("Phân loại theo vai trò:\n");
            byRole.forEach((role, count) ->
                sb.append("  • ").append(role).append(": ").append(count).append(" người\n")
            );

            // Danh sach nhan vien active
            sb.append("\nDanh sách nhân viên đang hoạt động:\n");
            staff.stream().filter(StaffFeignClient.StaffRow::active).forEach(s ->
                sb.append("  • [").append(s.role()).append("] ")
                  .append(s.fullName() != null ? s.fullName() : s.username())
                  .append("\n")
            );

            if (inactive > 0) {
                sb.append("\n⚠️ Có ").append(inactive).append(" tài khoản bị khóa.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getStaffWorkload error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu nhân viên. Có thể auth-service chưa khởi động hoặc không có quyền truy cập.";
        }
    }

    @Tool("Lấy tổng quan trạng thái menu hiện tại: số món hiện có, món hết hàng, món đang giảm giá/sale. " +
          "Trả về kết quả phân loại theo khu vực bếp (HOT/COLD/DRINK). " +
          "Dùng khi admin hỏi: 'món nào hết hàng', 'trạng thái menu', 'món nào đang sale', 'bếp có đủ nguyên liệu không'.")
    public String getMenuOverview() {
        log.info("[OPS-TOOL] getMenuOverview");
        try {
            var res = menuFeignClient.getMenuOverview();
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu trạng thái menu.";
            }

            var data = res.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("🍽️ TRẠNG THÁI MENU HIỆN TẠI:\n\n");
            sb.append("Tổng món đang hoạt động: ").append(data.totalActiveItems()).append("\n");
            sb.append("Đang giảm giá (sale): ").append(data.itemsOnSale()).append(" món\n");
            sb.append("Món nổi bật (featured): ").append(data.featuredItems()).append(" món\n");

            if (data.unavailableItems() == 0) {
                sb.append("\n✅ Tất cả món đều sẵn có — Không có món hết hàng!\n");
            } else {
                sb.append("\n⚠️ HẾT HÀNG: ").append(data.unavailableItems()).append(" món:\n");
                if (data.unavailableByStation() != null) {
                    data.unavailableByStation().forEach((station, count) ->
                        sb.append("  • ").append(station).append(": ").append(count).append(" món\n")
                    );
                }
                if (data.unavailableItemList() != null && !data.unavailableItemList().isEmpty()) {
                    sb.append("\nDanh sách cụ thể:\n");
                    data.unavailableItemList().forEach(item ->
                        sb.append("  - ").append(item.name())
                          .append(" [").append(item.station()).append("]")
                          .append(" — giá gốc: ").append(formatVnd(item.basePrice())).append("\n")
                    );
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getMenuOverview error: {}", e.getMessage());
            return "Lỗi khi lấy trạng thái menu. Vui lòng thử lại sau.";
        }
    }

    @Tool("Tổng hợp nhanh tình hình vận hành: kết hợp nhân sự + menu + staff call trong một báo cáo. " +
          "Dùng để trả lời câu hỏi 'tổng thể vận hành thế nào', 'có vấn đề gì không hôm nay'. " +
          "Tham số from/to định dạng yyyy-MM-dd.")
    public String getOperationalSummary(@P("Ngày bắt đầu (yyyy-MM-dd)") String from,
                                        @P("Ngày kết thúc (yyyy-MM-dd)") String to) {
        log.info("[OPS-TOOL] getOperationalSummary from={} to={}", from, to);
        try {
            LocalDate fromDate = parseDate(from);
            LocalDate toDate   = parseDate(to);
            if (fromDate == null || toDate == null) return "Lỗi: Ngày '" + from + "' hoặc '" + to + "' không đúng định dạng. Vui lòng dùng yyyy-MM-dd (ví dụ: 2026-05-26).";

            StringBuilder sb = new StringBuilder();
            sb.append("⚙️ TÓM TẮT VẬN HÀNH từ ").append(from).append(" đến ").append(to).append(":\n\n");

            // 1. Staff snapshot
            try {
                var staffRes = staffFeignClient.getAllStaff();
                if (staffRes != null && staffRes.getData() != null && staffRes.getData().getContent() != null) {
                    long active = staffRes.getData().getContent().stream().filter(StaffFeignClient.StaffRow::active).count();
                    sb.append("👥 Nhân sự: ").append(active).append(" đang hoạt động\n");
                }
            } catch (Exception ignored) {
                sb.append("👥 Nhân sự: không lấy được dữ liệu\n");
            }

            // 2. Menu snapshot
            try {
                var menuRes = menuFeignClient.getMenuOverview();
                if (menuRes != null && menuRes.getData() != null) {
                    long unavail = menuRes.getData().unavailableItems();
                    if (unavail > 0) {
                        sb.append("🍽️ Menu: ⚠️ ").append(unavail).append(" món hết hàng\n");
                    } else {
                        sb.append("🍽️ Menu: ✅ Tất cả món sẵn có\n");
                    }
                }
            } catch (Exception ignored) {
                sb.append("🍽️ Menu: không lấy được dữ liệu\n");
            }

            // 3. Staff calls
            try {
                var callRes = reportFeignClient.getStaffCallStats(fromDate, toDate, 1000);
                if (callRes != null && callRes.getData() != null && callRes.getData().getContent() != null) {
                    long totalCalls = callRes.getData().getContent().stream()
                            .mapToLong(ReportFeignClient.StaffCallRow::callCount).sum();
                    sb.append("📣 Gọi nhân viên: ").append(totalCalls).append(" lượt trong kỳ\n");
                }
            } catch (Exception ignored) {
                sb.append("📣 Gọi nhân viên: không lấy được dữ liệu\n");
            }

            // 4. Cancelled orders
            try {
                var cancelRes = reportFeignClient.getCancelledOrderDrilldown(fromDate, toDate, 1000);
                if (cancelRes != null && cancelRes.getData() != null && cancelRes.getData().getContent() != null && !cancelRes.getData().getContent().isEmpty()) {
                    long totalCancel = cancelRes.getData().getContent().stream()
                            .mapToLong(ReportFeignClient.CancelledRow::cancelCount).sum();
                    sb.append("🚫 Đơn hủy: ").append(totalCancel).append(" đơn\n");
                } else {
                    sb.append("🚫 Đơn hủy: ✅ Không có\n");
                }
            } catch (Exception ignored) {
                sb.append("🚫 Đơn hủy: không lấy được dữ liệu\n");
            }

            sb.append("\n💡 Để xem chi tiết, hãy hỏi thêm về từng mục: nhân sự, bếp, đơn hủy.");
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getOperationalSummary error: {}", e.getMessage());
            return "Lỗi khi tổng hợp vận hành. Vui lòng thử lại sau.";
        }
    }

    @Tool("Kiểm tra tình trạng chấm công (Attendance) của nhân viên trong ngày. " +
          "Dùng khi admin hỏi: 'ai đã chấm công', 'có ai đi trễ không', 'ai vắng mặt hôm nay'.")
    public String getStaffAttendanceStatus(@P("Ngày cần kiểm tra (yyyy-MM-dd)") String date) {
        log.info("[OPS-TOOL] getStaffAttendanceStatus date={}", date);
        try {
            var res = staffFeignClient.getAttendanceLogs(date, date);
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu chấm công.";
            }
            
            Object data = res.getData();
            if (data instanceof List<?> logs && !logs.isEmpty()) {
                StringBuilder sb = new StringBuilder("⏰ TÌNH TRẠNG CHẤM CÔNG NGÀY ").append(date).append(":\n\n");
                for (Object logObj : logs) {
                    if (logObj instanceof Map<?, ?> map) {
                        String user = (String) map.get("fullName");
                        if (user == null) user = (String) map.get("username");
                        String checkIn = (String) map.get("checkInTime");
                        String checkOut = (String) map.get("checkOutTime");
                        String status = (String) map.get("status"); // LATE, ON_TIME, etc.
                        
                        sb.append("• ").append(user).append(": ");
                        if (checkIn != null && checkIn.length() >= 16) sb.append("In: ").append(checkIn.substring(11, 16)).append(" ");
                        else if (checkIn != null) sb.append("In: ").append(checkIn).append(" ");
                        else sb.append("In: -- ");
                        
                        if (checkOut != null && checkOut.length() >= 16) sb.append("| Out: ").append(checkOut.substring(11, 16)).append(" ");
                        else if (checkOut != null) sb.append("| Out: ").append(checkOut).append(" ");
                        else sb.append("| Out: -- ");
                        
                        if (status != null) {
                            if ("LATE".equalsIgnoreCase(status)) sb.append(" ⚠️(Đi trễ)");
                            else if ("ON_TIME".equalsIgnoreCase(status)) sb.append(" ✅(Đúng giờ)");
                            else sb.append(" (").append(status).append(")");
                        }
                        sb.append("\n");
                    }
                }
                return sb.toString();
            } else {
                return "✅ Chưa có ai chấm công hoặc không có dữ liệu chấm công ngày " + date;
            }
        } catch (Exception e) {
            log.error("[OPS-TOOL] getStaffAttendanceStatus error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu chấm công: " + e.getMessage();
        }
    }

    @Tool("Lấy báo cáo tổng hợp chấm công của nhân viên từ ngày đến ngày (ví dụ: số giờ làm, đi muộn, về sớm). Format ngày: yyyy-MM-dd.")
    public String getAttendanceSummary(
            @P("Ngày bắt đầu (yyyy-MM-dd)") String from,
            @P("Ngày kết thúc (yyyy-MM-dd) - có thể bỏ trống nếu chỉ xem 1 ngày") String to
    ) {
        log.info("[OPS-TOOL] getAttendanceSummary from={} to={}", from, to);
        try {
            var res = staffFeignClient.getAttendanceSummary(from, to);
            if (res == null || res.getData() == null) {
                return "Không thể lấy tổng hợp chấm công.";
            }
            return "Dữ liệu tổng hợp chấm công: " + res.getData().toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getAttendanceSummary error: {}", e.getMessage());
            return "Lỗi khi lấy tổng hợp chấm công: " + e.getMessage();
        }
    }

    @Tool("Lấy lịch làm việc (Shift Schedules) của nhân viên trong ngày. " +
          "Dùng khi admin hỏi: 'lịch làm việc hôm nay', 'ai làm ca sáng', 'ca tối có mấy người'.")
    public String getShiftSchedules(@P("Ngày cần xem lịch (yyyy-MM-dd)") String date) {
        log.info("[OPS-TOOL] getShiftSchedules date={}", date);
        try {
            var res = staffFeignClient.getSchedules(date, date);
            if (res == null || res.getData() == null) {
                return "Không thể lấy dữ liệu lịch làm việc.";
            }
            
            Object data = res.getData();
            if (data instanceof List<?> schedules && !schedules.isEmpty()) {
                StringBuilder sb = new StringBuilder("📅 LỊCH LÀM VIỆC NGÀY ").append(date).append(":\n\n");
                // Nhom theo shiftName
                Map<String, List<Map<?, ?>>> byShift = schedules.stream()
                    .filter(obj -> obj instanceof Map<?, ?>)
                    .map(obj -> (Map<?, ?>) obj)
                    .collect(Collectors.groupingBy(map -> {
                        Object shift = map.get("shiftTemplate");
                        if (shift instanceof Map<?, ?> s) {
                            String name = s.get("name") != null ? String.valueOf(s.get("name")) : "Ca khác";
                            String start = s.get("startTime") != null ? String.valueOf(s.get("startTime")) : "?";
                            String end = s.get("endTime") != null ? String.valueOf(s.get("endTime")) : "?";
                            return name + " (" + start + " - " + end + ")";
                        }
                        return "Ca khác";
                    }));
                
                byShift.forEach((shiftName, list) -> {
                    sb.append("🔹 ").append(shiftName).append(" (").append(list.size()).append(" người):\n");
                    for (Map<?, ?> map : list) {
                        Object userObj = map.get("user");
                        if (userObj instanceof Map<?, ?> user) {
                            String name = (String) user.get("fullName");
                            if (name == null) name = (String) user.get("username");
                            String role = (String) user.get("role");
                            sb.append("   - [").append(role).append("] ").append(name).append("\n");
                        }
                    }
                    sb.append("\n");
                });
                return sb.toString();
            } else {
                return "Chưa có ai được phân ca làm việc ngày " + date;
            }
        } catch (Exception e) {
            log.error("[OPS-TOOL] getShiftSchedules error: {}", e.getMessage());
            return "Lỗi khi lấy dữ liệu lịch làm việc: " + e.getMessage();
        }
    }

    @Tool("Phân tích và đánh giá tải trọng nhân sự (Staff Workload) hiện tại. " +
          "So sánh giữa số lượng nhân viên đang có mặt và lượng khách hoặc đơn hàng.")
    public String analyzeStaffWorkload() {
        log.info("[OPS-TOOL] analyzeStaffWorkload");
        return "Theo đánh giá, hệ thống đang trong giai đoạn thu thập thêm dữ liệu lượng khách. " +
               "Để biết nhân sự có đủ hay không, hãy gọi tool 'getStaffAttendanceStatus' để xem số người đang làm, " +
               "và dùng 'getActiveKitchenTickets' hoặc kiểm tra số lượng bàn đang hoạt động.";
    }

    @Tool("Lấy danh sách nhân viên chi tiết kèm ID (dùng để lấy userId trước khi phân ca làm việc).")
    public String getStaffList() {
        log.info("[OPS-TOOL] getStaffList");
        try {
            var res = staffFeignClient.getAllStaff();
            if (res == null || res.getData() == null || res.getData().getContent().isEmpty()) {
                return "Không có dữ liệu nhân viên.";
            }
            StringBuilder sb = new StringBuilder("📋 DANH SÁCH NHÂN VIÊN:\n\n");
            res.getData().getContent().forEach(s -> 
                sb.append("- ").append(s.fullName() != null ? s.fullName() : s.username())
                  .append(" | Vai trò: ").append(s.role())
                  .append(" | Trạng thái: ").append(s.active() ? "Đang hoạt động" : "Bị khóa")
                  .append(" | ID: ").append(s.id()).append("\n")
            );
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] Loi lay kpi: {}", e.getMessage());
            return "Không lấy được báo cáo KPI: " + e.getMessage();
        }
    }

    @Tool("Lấy danh sách và thống kê trạng thái các bàn hiện tại trong nhà hàng. Dùng khi admin hỏi: 'Sảnh đang có bao nhiêu khách', 'Còn bàn trống không', 'Bàn nào đang trống'.")
    public String getTableStatus() {
        log.info("[OPS-TOOL] getTableStatus");
        try {
            var res = orderFeignClient.getAllTablesForPos();
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Không có dữ liệu bàn trên hệ thống.";
            }

            List<com.fnb.ai.feign.OrderFeignClient.PosTableRow> tables = res.getData();
            long total = tables.size();
            long free = tables.stream().filter(t -> "FREE".equalsIgnoreCase(t.status())).count();
            long occupied = tables.stream().filter(t -> "OCCUPIED".equalsIgnoreCase(t.status())).count();
            long other = total - free - occupied;

            StringBuilder sb = new StringBuilder();
            sb.append("TỔNG QUAN SẢNH HIỆN TẠI:\n");
            sb.append("- Tổng số bàn: ").append(total).append("\n");
            sb.append("- Bàn đang có khách (OCCUPIED): ").append(occupied).append("\n");
            sb.append("- Bàn trống (FREE): ").append(free).append("\n");
            sb.append("- Trạng thái khác (Dọn dẹp/Chờ TT...): ").append(other).append("\n\n");

            if (occupied > 0) {
                sb.append("Danh sách bàn đang có khách: ");
                sb.append(tables.stream().filter(t -> "OCCUPIED".equalsIgnoreCase(t.status()))
                        .map(t -> t.name()).collect(Collectors.joining(", ")));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] Loi lay table status: {}", e.getMessage());
            return "Lỗi khi lấy trạng thái bàn: " + e.getMessage();
        }
    }

    @Tool("Lấy danh sách các chuông gọi phục vụ từ khách hàng mà nhân viên chưa xử lý xong (WATER, BILL, CLEAN, SUPPORT). Dùng khi admin hỏi: 'Có bàn nào đang gọi không', 'Nhân viên có ra kịp không', 'Chuông gọi sảnh'.")
    public String getStaffCalls() {
        log.info("[OPS-TOOL] getStaffCalls");
        try {
            var res = orderFeignClient.getActiveStaffCalls();
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Hiện tại KHÔNG CÓ chuông gọi phục vụ nào đang chờ xử lý. Sảnh đang hoạt động ổn định.";
            }

            List<Object> calls = res.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("HIỆN ĐANG CÓ ").append(calls.size()).append(" YÊU CẦU PHỤC VỤ CHƯA ĐƯỢC XỬ LÝ XONG:\n");
            // Vì Object dạng Map từ JSON, ta có thể stringify nó
            sb.append(calls.toString());
            
            sb.append("\nKhuyến nghị: Hãy nhắc nhở nhân viên sảnh (Server) ra bàn xử lý ngay để tránh ảnh hưởng trải nghiệm khách.");
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] Loi lay staff calls: {}", e.getMessage());
            return "Lỗi khi lấy danh sách chuông gọi phục vụ: " + e.getMessage();
        }
    }

    @Tool("Lấy danh sách các ca làm việc mẫu (Shift Templates) kèm ID (dùng để lấy shiftId trước khi phân ca).")
    public String getShiftTemplates() {
        log.info("[OPS-TOOL] getShiftTemplates");
        try {
            var res = staffFeignClient.getAllShifts();
            if (res == null || res.getData() == null) return "Không có dữ liệu ca làm mẫu.";
            return "Danh sách ca làm việc mẫu: " + res.getData().toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getShiftTemplates error: {}", e.getMessage());
            return "Lỗi khi lấy danh sách ca làm việc mẫu: " + e.getMessage();
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String formatVnd(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,.0f", amount) + "đ";
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
