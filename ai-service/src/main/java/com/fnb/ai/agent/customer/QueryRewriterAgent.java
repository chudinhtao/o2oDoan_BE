package com.fnb.ai.agent.customer;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * Agent phụ trách việc viết lại câu hỏi dựa vào ngữ cảnh (Query Rewriting).
 * Giúp biến các câu hỏi phụ thuộc ngữ cảnh thành câu hỏi độc lập (Standalone Query)
 * để tăng cường độ chính xác cho Semantic Cache.
 */
@AiService(wiringMode = dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT,
           chatModel = "fastChatModel",
           chatMemoryProvider = "chatMemoryProvider")
public interface QueryRewriterAgent {

    @SystemMessage({
        "Bạn là một trợ lý viết lại câu hỏi (Query Rewriter) cho hệ thống tra cứu ẩm thực.",
        "Nhiệm vụ của bạn: Dựa vào LỊCH SỬ HỘI THOẠI được cung cấp bên dưới, hãy viết lại CÂU HỎI MỚI NHẤT của người dùng thành một CÂU ĐỘC LẬP duy nhất, rõ nghĩa, không chứa các đại từ chỉ định (như: này, đó, kia, cái này).",
        "Ví dụ 1:",
        "H: Tôi thích ăn cay.",
        "U: Gợi ý món ăn cho tôi.",
        "-> Gợi ý món ăn cay cho tôi.",
        "Ví dụ 2:",
        "H: Lấy tôi 1 sườn nướng.",
        "U: Món này có béo không?",
        "-> Món sườn nướng có béo không?",
        "Quy tắc:",
        "1. Nếu câu hỏi của người dùng đã đủ ý nghĩa, hãy giữ nguyên câu hỏi đó.",
        "2. TUYỆT ĐỐI KHÔNG trả lời câu hỏi, chỉ có nhiệm vụ VIẾT LẠI câu hỏi.",
        "3. Nếu câu hỏi là lệnh thao tác không cần tra cứu (như Đặt món, Tính tiền, Gọi phục vụ), hãy trả về: [ACTION_REQUIRED]",
        "",
        "--- LỊCH SỬ HỘI THOẠI GẦN ĐÂY ---",
        "{{history}}",
        "----------------------------------"
    })
    String rewrite(@dev.langchain4j.service.V("history") String history, @UserMessage String userMessage);
}
