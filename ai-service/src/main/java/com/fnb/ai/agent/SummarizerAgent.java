package com.fnb.ai.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(wiringMode = dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT,
           chatModel = "fastChatModel",
           chatMemoryProvider = "chatMemoryProvider",
           tools = {})
public interface SummarizerAgent {
    
    @SystemMessage("Bạn là trợ lý hệ thống. Nhiệm vụ của bạn là đọc đoạn hội thoại sau và tóm tắt nó thành 1 hoặc 2 câu cực kỳ ngắn gọn. " +
                   "Chỉ giữ lại các thông tin quan trọng như: sở thích của khách, món khách đang quan tâm, món khách đã gọi, hoặc tên khách. " +
                   "TUYỆT ĐỐI không thêm thắt thông tin. Bạn đang viết tóm tắt để cung cấp ngữ cảnh cho lượt chat tiếp theo của AI.")
    String summarize(@dev.langchain4j.service.MemoryId String memoryId, @UserMessage String conversationHistory);
}
