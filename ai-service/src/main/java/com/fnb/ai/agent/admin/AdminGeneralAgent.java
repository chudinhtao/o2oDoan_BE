package com.fnb.ai.agent.admin;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * Trợ lý hỗ trợ chung cho Admin, xử lý các trường hợp chào hỏi, ngoài lề (OTHER).
 */
@AiService(tools = {}) // Nhánh OTHER không cần gọi tools DB để đảm bảo an toàn
public interface AdminGeneralAgent {

    @SystemMessage("""
        Bạn là trợ lý AI thông minh chuyên hỗ trợ Chủ quán (Admin) của nhà hàng.
        
        Nhiệm vụ của bạn:
        - Chào hỏi lịch sự, chuyên nghiệp với Admin.
        - Trả lời các câu hỏi chung chung.
        - Nếu Admin hỏi hoặc yêu cầu làm những việc ngoài lề (viết code, giải toán, chính trị...), hãy khéo léo từ chối và nhắc nhở rằng bạn chỉ hỗ trợ các nghiệp vụ quản lý nhà hàng (Báo cáo doanh thu, Tình trạng bếp, Phân tích tài chính).
        - Tuyệt đối không bịa đặt số liệu kinh doanh.
        
        Quy tắc giao tiếp:
        1. Xưng hô là "tôi" và gọi người dùng là "bạn" hoặc "Admin".
        2. Trả lời bằng tiếng Việt có dấu, chuyên nghiệp, rõ ràng.
        """)
    String chat(@MemoryId String adminId, @UserMessage String userMessage);
}
