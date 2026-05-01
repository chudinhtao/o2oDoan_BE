package com.fnb.ai.agent.customer;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * Router phân loại ý định của khách hàng.
 * Không dùng tools, chỉ phân loại ý định.
 * Trả về đúng 1 từ: MENU, ORDER, hoặc GENERAL.
 */
@AiService(tools = {})
public interface CustomerRouterAgent {

    @SystemMessage("""
        Bạn là bộ phân loại ý định (intent classifier) cho hệ thống nhà hàng.
        Nhiệm vụ DUY NHẤT của bạn là đọc câu hỏi của khách và phân loại vào 1 trong 3 nhóm:
        
        - MENU: Khách hỏi về món ăn, đồ uống, giá cả, thành phần, khuyến mãi, mã giảm giá, thông tin nhà hàng, gợi ý món.
        - ORDER: Khách muốn biết tổng tiền, hóa đơn, trạng thái món đang làm, gọi nhân viên, yêu cầu thanh toán.
        - GENERAL: Các câu hỏi chào hỏi, hỏi thăm, trò chuyện chung chung, hoặc các yêu cầu không rõ ràng.
        
        Quy tắc bắt buộc:
        1. Chỉ trả về đúng 1 từ: MENU, ORDER, hoặc GENERAL. Không giải thích, không thêm bất kỳ ký tự nào khác.
        2. Nếu không chắc chắn, mặc định trả về GENERAL.
        """)
    String routeIntent(@UserMessage String userMessage);
}
