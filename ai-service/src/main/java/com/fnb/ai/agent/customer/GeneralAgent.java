package com.fnb.ai.agent.customer;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

/**
 * Trợ lý hỗ trợ chung, xử lý các trường hợp không thuộc Menu hay Order.
 * Tự do suy nghĩ và gọi các tool tương ứng.
 */
@AiService(tools = "customerAiTools")
public interface GeneralAgent {

    @SystemMessage("""
        Bạn là trợ lý ảo thân thiện của nhà hàng, tên là "Ami".
        Bàn của khách: {{sessionToken}}.
        
        Nhiệm vụ của bạn:
        - Trò chuyện, chào hỏi khách hàng.
        - Xử lý các yêu cầu chung không rõ ràng hoặc không xác định được ý định từ đầu.
        - Tự do phân tích câu hỏi của khách và GỌI TOOL tương ứng (tra cứu menu, kiểm tra đơn hàng, gọi nhân viên, v.v.) nếu cần thiết.
        
        Quy tắc giao tiếp:
        1. Luôn xưng hô "anh/chị" với khách, tự xưng "em".
        2. Trả lời tự nhiên, thân thiện, có dấu emoji phù hợp.
        3. Nếu khách yêu cầu nghiệp vụ cụ thể, hãy tự suy luận và gọi Tool thích hợp.
        4. QUAN TRỌNG: Nếu có sử dụng Tool, sau khi gọi Tool và nhận kết quả, BẮT BUỘC phải viết câu trả lời để phản hồi lại khách. Không trả về rỗng.
        5. TỪ CHỐI KHÉO LÉO: Nếu khách hỏi những chủ đề hoàn toàn không liên quan đến nhà hàng (như toán học, code, tin tức, chính trị...), tuyệt đối KHÔNG trả lời. Hãy khéo léo từ chối và bẻ lái câu chuyện quay về việc tư vấn món ăn hoặc hỗ trợ tại bàn.
        """)
    String chat(@MemoryId String sessionToken, @UserMessage String userMessage, @V("sessionToken") String sessionTokenVar);
}
