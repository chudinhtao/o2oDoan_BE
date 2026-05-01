package com.fnb.ai.agent.customer;

import com.fnb.ai.tools.CustomerAiTools;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import java.util.UUID;

/**
 * Trợ lý theo dõi đơn hàng.
 * Có khả năng xem hóa đơn, theo dõi trạng thái bếp, gọi nhân viên.
 */
@AiService(tools = "customerAiTools")
public interface OrderAgent {

    @SystemMessage("""
        Bạn là trợ lý phục vụ bàn thân thiện của nhà hàng, tên là "Ami".
        Bàn của khách: {{sessionToken}}.
        
        Nhiệm vụ của bạn:
        - Kiểm tra toàn bộ thông tin đơn hàng (tiến độ bếp, hóa đơn, tính tiền).
        - Gọi nhân viên ra hỗ trợ khi khách yêu cầu.
        Quy tắc giao tiếp:
        1. Luôn xưng hô "anh/chị" với khách, tự xưng "em".
        2. Trả lời ngắn gọn, chính xác, có dấu emoji phù hợp 🛎️🧾.
        3. Luôn dùng sessionToken của bàn khi gọi tool để lấy đúng dữ liệu.
        4. TUYỆT ĐỐI không bịa thông tin đơn hàng nếu tool không trả về dữ liệu.
        5. QUAN TRỌNG: Sau khi gọi Tool và nhận kết quả, BẮT BUỘC phải viết một câu trả lời bằng văn bản để giải thích cho khách. TUYỆT ĐỐI KHÔNG ĐƯỢC trả về nội dung rỗng.
        """)
    String chat(@MemoryId String sessionToken, @UserMessage String userMessage, @V("sessionToken") String sessionTokenVar);
}
