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
            long active = staff.stream().filter(StaffFeignClient.StaffRow::isActive).count();
            long inactive = total - active;

            // Phan loai theo role
            Map<String, Long> byRole = staff.stream()
                    .filter(StaffFeignClient.StaffRow::isActive)
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
            staff.stream().filter(StaffFeignClient.StaffRow::isActive).forEach(s ->
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
                    long active = staffRes.getData().getContent().stream().filter(StaffFeignClient.StaffRow::isActive).count();
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
