package com.fnb.ai.agent.admin;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * [PHASE 4 — ACTIVE] Multi-Agent Orchestrator cho Admin AI.
 * 
 * - [Phase 4.1] LLM Router thay cho Regex de phan loai (FINANCE, OPS, REPORT).
 * - [Phase 4.2] Semantic Caching (TTL: 15 phut) de giam token va thoi gian doi.
 * - Regex van duoc dung de loai tru cac yeu cau CRUD (nhu them mon, sua bang...).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrchestrator {

    private final AdminReportAgent reportAgent;
    private final FinanceAgent financeAgent;
    private final OperationalAgent operationalAgent;
    private final AdminGeneralAgent generalAgent;
    
    // Phase 4 additions
    private final AdminRouterAgent routerAgent;
    private final JdbcTemplate jdbc;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
    private final dev.langchain4j.memory.chat.ChatMemoryProvider chatMemoryProvider;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String processChat(String adminId, String userMessage) {
        String msgLower = userMessage.toLowerCase().trim();
        String msgUnaccented = removeAccents(msgLower);

        // 1. Kiem tra cac yeu cau CRUD bang Regex de bao loi som (khong dung AI)
        String crudCheck = checkCrudIntents(msgUnaccented);
        if (crudCheck != null) {
            return crudCheck;
        }


        // 3. [Phase 4.1] Dung LLM Router de phan loai (Cache MISS)
        String routerMemoryId = "stateless-router-" + UUID.randomUUID().toString();
        
        // Lay lich su gan nhat de Router hieu anaphora ("mã gì", "nó")
        String historyContext = "";
        try {
            dev.langchain4j.memory.ChatMemory memory = chatMemoryProvider.get(adminId);
            if (memory != null && memory.messages() != null) {
                int size = memory.messages().size();
                int start = Math.max(0, size - 4); // Lay 4 tin nhan gan nhat
                StringBuilder sb = new StringBuilder();
                for (int i = start; i < size; i++) {
                    dev.langchain4j.data.message.ChatMessage msg = memory.messages().get(i);
                    sb.append(msg.type().name()).append(": ");
                    if (msg instanceof dev.langchain4j.data.message.UserMessage u) {
                        sb.append(u.singleText());
                    } else if (msg instanceof dev.langchain4j.data.message.AiMessage a) {
                        sb.append(a.text());
                    } else {
                        sb.append(msg.toString());
                    }
                    sb.append("\n");
                }
                historyContext = sb.toString();
            }
        } catch (Exception e) {
            log.warn("[ADMIN-ORCHESTRATOR] Khong the lay lich su chat cho router: {}", e.getMessage());
        }

        String domain = routerAgent.routeIntent(routerMemoryId, userMessage, historyContext).trim().toUpperCase();
        log.info("[ADMIN-ORCHESTRATOR] adminId={} | domain={} | msg={}", adminId, domain, userMessage);

        TimeContext tc = buildTimeContext();

        // Inject thoi gian thuc vao user message de AI luon co ngay chinh xac
        // (tranh hallucinate ngay khi session memory cu khong co date context)
        String dateContext = String.format(
            "[NGAY HOM NAY: %s (%s) | HOM QUA: %s | TUAN NAY: %s den %s | THANG NAY: %s den %s]",
            tc.today, tc.dayOfWeek, tc.yesterday,
            tc.weekStart, tc.today,
            tc.monthStart, tc.today
        );
        String enrichedMessage = dateContext + "\n" + userMessage;

        String aiResponse;

        switch (domain) {
            case "FINANCE":
                aiResponse = financeAgent.chat(
                        adminId, enrichedMessage,
                        tc.today, tc.monthStart, tc.lastMonthStart, tc.lastMonthEnd, tc.sevenDaysAgo
                );
                break;
            case "OPS":
                aiResponse = operationalAgent.chat(
                        adminId, enrichedMessage,
                        tc.today, tc.yesterday, tc.weekStart, tc.monthStart, tc.sevenDaysAgo
                );
                break;
            case "REPORT":
                aiResponse = reportAgent.chat(
                        adminId, enrichedMessage,
                        tc.today, tc.dayOfWeek, tc.yesterday, tc.weekStart, tc.lastWeekStart, tc.lastWeekEnd, tc.monthStart, tc.lastMonthStart, tc.lastMonthEnd, tc.sevenDaysAgo
                );
                break;
            case "GREET":
                aiResponse = generalAgent.chat(adminId, userMessage);
                break;
            case "OUT_OF_SCOPE":
                // Hardcode — không gọi LLM để tránh bị bypass
                return "Xin lỗi, tôi chỉ hỗ trợ các nghiệp vụ quản lý nhà hàng như: " +
                        "Báo cáo doanh thu, Phân tích tài chính, Vận hành bếp và Quản lý menu. " +
                        "Câu hỏi của bạn nằm ngoài phạm vi hỗ trợ của tôi. " +
                        "Bạn có muốn xem báo cáo kinh doanh hôm nay không?";
            default:
                aiResponse = generalAgent.chat(adminId, userMessage);
                break;
        }



        return aiResponse;
    }

    /**
     * Kiem tra som cac the loai CRUD va bao loi (Vi AI khong the thuc hien ghi du lieu).
     * Yêu cầu truyền vào chuỗi KHÔNG DẤU.
     * @return Chuoi canh bao neu la CRUD, null neu la cau hoi binh thuong
     */
    private String checkCrudIntents(String m) {
        if (m.matches(".*(them nhan vien|tao tai khoan|khoa acc|mo khoa|doi mat khau|quan ly nhan vien).*"))
            return "Việc quản lý nhân viên (Thêm, xóa, sửa) vui lòng thực hiện trên giao diện Quản lý Nhân viên. Tôi chỉ hỗ trợ xem trạng thái đang trực hoặc khảo sát hiệu suất.";

        if (m.matches(".*(so do ban|tao ban|them ban|qr code|reset qr|ban so).*") && !m.matches(".*(doanh thu ban|ban nao hieu qua|ban dung lau).*"))
            return "Việc quản lý sơ đồ bàn và in QR code, vui lòng thao tác tại màn hình Quản lý Bàn.";

        if (m.matches(".*(an mon|hien mon|sua mon|them mon moi|tao mon|xoa mon|gia ban|cap nhat gia).*") && !m.matches(".*(het hang|mon nao ban chay|trang thai menu|mon nao dang sale).*"))
            return "Để thay đổi Menu (thêm món, sửa giá, ẩn/hiển), vui lòng thao tác ở màn hình Quản lý Menu. Tôi có thể giúp bạn kiểm tra tình trạng các món đang hết hàng hoặc phân tích menu (BCG Matrix).";

        if (m.matches(".*(tao ma km|tao khuyen mai|xoa km|cap nhat km|bat km|tat km).*"))
            return "Để tạo hoặc hủy chương trình Khuyến mãi, vui lòng truy cập module Quản lý Khuyến Mãi. Tôi chỉ có thể hỗ trợ phân tích độ hiệu quả (ROI) của các khuyến mãi hiện tại.";

        return null;
    }

    /**
     * Helper: Xóa dấu tiếng Việt để match Regex dễ dàng hơn
     */
    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized)
                .replaceAll("").replace("đ", "d").replace("Đ", "D");
    }

    private static final java.time.ZoneId VN_ZONE = java.time.ZoneId.of("Asia/Ho_Chi_Minh");

    private TimeContext buildTimeContext() {
        LocalDate today = LocalDate.now(VN_ZONE);
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        LocalDate lastWeekEnd = weekStart.minusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);
        LocalDate lastMonthEnd = monthStart.minusDays(1);
        LocalDate sevenDaysAgo = today.minusDays(7);

        String dayOfWeekVn = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.of("vi"));

        return new TimeContext(
            today.format(DATE_FMT),
            dayOfWeekVn,
            yesterday.format(DATE_FMT),
            weekStart.format(DATE_FMT),
            lastWeekStart.format(DATE_FMT),
            lastWeekEnd.format(DATE_FMT),
            monthStart.format(DATE_FMT),
            lastMonthStart.format(DATE_FMT),
            lastMonthEnd.format(DATE_FMT),
            sevenDaysAgo.format(DATE_FMT)
        );
    }

    private record TimeContext(
            String today,
            String dayOfWeek,
            String yesterday,
            String weekStart,
            String lastWeekStart,
            String lastWeekEnd,
            String monthStart,
            String lastMonthStart,
            String lastMonthEnd,
            String sevenDaysAgo
    ) {}
}
