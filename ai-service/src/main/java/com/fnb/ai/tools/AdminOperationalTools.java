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

    @Tool("Lay danh sach nhan vien va phan tich tai trong: so luong theo vai tro (CASHIER/KITCHEN/ADMIN), " +
          "ai dang hoat dong, ai bi khoa. " +
          "Dung khi admin hoi: 'bao nhieu nhan vien', 'staff hien tai', 'kiem tra nhan su', 'thieu nguoi khong'.")
    public String getStaffWorkload() {
        log.info("[OPS-TOOL] getStaffWorkload");
        try {
            var res = staffFeignClient.getAllStaff();
            if (res == null || res.getData() == null || res.getData().isEmpty()) {
                return "Khong co du lieu nhan vien.";
            }

            List<StaffFeignClient.StaffRow> staff = res.getData();
            long total  = staff.size();
            long active = staff.stream().filter(StaffFeignClient.StaffRow::isActive).count();
            long inactive = total - active;

            // Phan loai theo role
            java.util.Map<String, Long> byRole = staff.stream()
                    .filter(StaffFeignClient.StaffRow::isActive)
                    .collect(java.util.stream.Collectors.groupingBy(
                            s -> s.role() != null ? s.role() : "UNKNOWN",
                            java.util.stream.Collectors.counting()
                    ));

            StringBuilder sb = new StringBuilder();
            sb.append("👥 NHAN SU HIEN TAI:\n\n");
            sb.append("Tong so: ").append(total)
              .append(" nhan vien | Dang hoat dong: ").append(active)
              .append(" | Bi khoa: ").append(inactive).append("\n\n");
            sb.append("Phan loai theo vai tro:\n");
            byRole.forEach((role, count) ->
                sb.append("  • ").append(role).append(": ").append(count).append(" nguoi\n")
            );

            // Danh sach nhan vien active
            sb.append("\nDanh sach nhan vien dang hoat dong:\n");
            staff.stream().filter(StaffFeignClient.StaffRow::isActive).forEach(s ->
                sb.append("  • [").append(s.role()).append("] ")
                  .append(s.fullName() != null ? s.fullName() : s.username())
                  .append("\n")
            );

            if (inactive > 0) {
                sb.append("\n⚠️ Co ").append(inactive).append(" tai khoan bi khoa.");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getStaffWorkload error: {}", e.getMessage());
            return "Loi khi lay du lieu nhan vien. Co the auth-service chua khoi dong hoac khong co quyen truy cap.";
        }
    }

    @Tool("Lay tong quan trang thai menu hien tai: so mon hien co, mon het hang, mon dang giam gia/sale. " +
          "Tra ve ket qua phan loai theo khu vuc bep (HOT/COLD/DRINK). " +
          "Dung khi admin hoi: 'mon nao het hang', 'trang thai menu', 'mon nao dang sale', 'bep co du nguyen lieu khong'.")
    public String getMenuOverview() {
        log.info("[OPS-TOOL] getMenuOverview");
        try {
            var res = menuFeignClient.getMenuOverview();
            if (res == null || res.getData() == null) {
                return "Khong the lay du lieu trang thai menu.";
            }

            var data = res.getData();
            StringBuilder sb = new StringBuilder();
            sb.append("🍽️ TRANG THAI MENU HIEN TAI:\n\n");
            sb.append("Tong mon dang hoat dong: ").append(data.totalActiveItems()).append("\n");
            sb.append("Dang giam gia (sale): ").append(data.itemsOnSale()).append(" mon\n");
            sb.append("Mon noi bat (featured): ").append(data.featuredItems()).append(" mon\n");

            if (data.unavailableItems() == 0) {
                sb.append("\n✅ Tat ca mon deu san co — Khong co mon het hang!\n");
            } else {
                sb.append("\n⚠️ HET HANG: ").append(data.unavailableItems()).append(" mon:\n");
                if (data.unavailableByStation() != null) {
                    data.unavailableByStation().forEach((station, count) ->
                        sb.append("  • ").append(station).append(": ").append(count).append(" mon\n")
                    );
                }
                if (data.unavailableItemList() != null && !data.unavailableItemList().isEmpty()) {
                    sb.append("\nDanh sach cu the:\n");
                    data.unavailableItemList().forEach(item ->
                        sb.append("  - ").append(item.name())
                          .append(" [").append(item.station()).append("]")
                          .append(" — gia goc: ").append(formatVnd(item.basePrice())).append("\n")
                    );
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getMenuOverview error: {}", e.getMessage());
            return "Loi khi lay trang thai menu. Vui long thu lai sau.";
        }
    }

    @Tool("Tong hop nhanh tinh hinh van hanh: ket hop nhan su + menu + staff call trong mot bao cao. " +
          "Dung de tra loi cau hoi 'tong the van hanh the nao', 'co van de gi khong hom nay'. " +
          "Tham so from/to dinh dang yyyy-MM-dd.")
    public String getOperationalSummary(@P("Ngay bat dau (yyyy-MM-dd)") String from,
                                        @P("Ngay ket thuc (yyyy-MM-dd)") String to) {
        log.info("[OPS-TOOL] getOperationalSummary from={} to={}", from, to);
        try {
            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate   = LocalDate.parse(to);

            StringBuilder sb = new StringBuilder();
            sb.append("⚙️ TOM TAT VAN HANH tu ").append(from).append(" den ").append(to).append(":\n\n");

            // 1. Staff snapshot
            try {
                var staffRes = staffFeignClient.getAllStaff();
                if (staffRes != null && staffRes.getData() != null) {
                    long active = staffRes.getData().stream().filter(StaffFeignClient.StaffRow::isActive).count();
                    sb.append("👥 Nhan su: ").append(active).append(" dang hoat dong\n");
                }
            } catch (Exception ignored) {
                sb.append("👥 Nhan su: khong lay duoc du lieu\n");
            }

            // 2. Menu snapshot
            try {
                var menuRes = menuFeignClient.getMenuOverview();
                if (menuRes != null && menuRes.getData() != null) {
                    long unavail = menuRes.getData().unavailableItems();
                    if (unavail > 0) {
                        sb.append("🍽️ Menu: ⚠️ ").append(unavail).append(" mon het hang\n");
                    } else {
                        sb.append("🍽️ Menu: ✅ Tat ca mon san co\n");
                    }
                }
            } catch (Exception ignored) {
                sb.append("🍽️ Menu: khong lay duoc du lieu\n");
            }

            // 3. Staff calls
            try {
                var callRes = reportFeignClient.getStaffCallStats(fromDate, toDate);
                if (callRes != null && callRes.getData() != null) {
                    long totalCalls = callRes.getData().stream()
                            .mapToLong(ReportFeignClient.StaffCallRow::callCount).sum();
                    sb.append("📣 Goi nhan vien: ").append(totalCalls).append(" luot trong ky\n");
                }
            } catch (Exception ignored) {
                sb.append("📣 Goi nhan vien: khong lay duoc du lieu\n");
            }

            // 4. Cancelled orders
            try {
                var cancelRes = reportFeignClient.getCancelledOrderDrilldown(fromDate, toDate);
                if (cancelRes != null && cancelRes.getData() != null && !cancelRes.getData().isEmpty()) {
                    long totalCancel = cancelRes.getData().stream()
                            .mapToLong(ReportFeignClient.CancelledRow::cancelCount).sum();
                    sb.append("🚫 Don huy: ").append(totalCancel).append(" don\n");
                } else {
                    sb.append("🚫 Don huy: ✅ Khong co\n");
                }
            } catch (Exception ignored) {
                sb.append("🚫 Don huy: khong lay duoc du lieu\n");
            }

            sb.append("\n💡 De xem chi tiet, hay hoi them ve tung muc: nhan su, bep, don huy.");
            return sb.toString();
        } catch (Exception e) {
            log.error("[OPS-TOOL] getOperationalSummary error: {}", e.getMessage());
            return "Loi khi tong hop van hanh. Vui long thu lai sau.";
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    private String formatVnd(java.math.BigDecimal amount) {
        if (amount == null) return "0d";
        return String.format("%,.0f", amount) + "d";
    }
}
