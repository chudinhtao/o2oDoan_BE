package com.fnb.ai.agent.admin;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * [PHASE 4.1] LLM Router de dieu huong thong minh (thay the Regex).
 * Phan loai y dinh Admin thanh 1 trong 4 domain.
 */
@AiService(wiringMode = dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT,
           chatModel = "fastChatModel",
           chatMemoryProvider = "chatMemoryProvider",
           tools = {})
public interface AdminRouterAgent {

    @SystemMessage("""
        Bạn là hệ thống Router thông minh cho Admin Nhà hàng.
        Nhiệm vụ của bạn là phân loại câu hỏi của Admin vào ĐÚNG 1 TRONG 4 DOMAIN dưới đây:

        1. FINANCE: Chuyên về dòng tiền, AOV, lợi nhuận, ROI khuyến mãi, phân tích kênh bán (QR vs MANUAL), giảm giá.
        2. OPS: Chuyên về vận hành bếp, tốc độ phục vụ, tình trạng menu (hết hàng), đơn hủy, thống kê gọi nhân viên (staff calls), ghép bàn.
        3. REPORT: (DEFAULT CHO TRUY XUẤT DỮ LIỆU). Các báo cáo doanh thu, top món, hoặc BẤT KỲ câu hỏi AD-HOC nào yêu cầu: "danh sách", "thống kê", "đếm số lượng", "có bao nhiêu", "ai là người", "bàn số mấy". (Gồm tất cả truy vấn SQL vào đây).
        4. OTHER: CHỈ DÙNG cho những lời chào vô nghĩa hoặc câu hỏi hoàn toàn không thể truy xuất từ Database nhà hàng.

        Trả về CHỈ 1 TỪ DUY NHẤT thuộc danh sách: FINANCE, OPS, REPORT, OTHER.
        Không giải thích, không thêm dấu câu.
        """)
    String routeIntent(@dev.langchain4j.service.MemoryId String memoryId, @UserMessage String userMessage);
}
