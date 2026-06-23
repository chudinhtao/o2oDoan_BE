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
        You are a routing classifier for a Vietnamese restaurant admin system.
        Classify the input into EXACTLY ONE of these 5 labels:

        FINANCE  - revenue, profit, AOV, ROI, promotions, payment channels
        OPS      - kitchen speed, out-of-stock, cancelled orders, staff calls, table management, inventory
        REPORT   - any data retrieval: lists, counts, statistics, rankings, "how many", "who is"
        GREET    - short greetings only: hello, thanks, goodbye, "ban la ai"
        OUT_OF_SCOPE - anything unrelated to restaurant operations: coding, math, poetry, weather, sports, health, politics, translation, general knowledge

        EXAMPLES:
        "doanh thu hom nay" -> REPORT
        "tinh hinh kho" -> OPS
        "roi khuyen mai thang nay" -> FINANCE
        "xin chao" -> GREET
        "viet code python" -> OUT_OF_SCOPE
        "code chuong trinh" -> OUT_OF_SCOPE
        "giai phuong trinh" -> OUT_OF_SCOPE
        "thoi tiet hom nay" -> OUT_OF_SCOPE
        "top mon ban chay" -> REPORT
        "bep co bi tre khong" -> OPS
        "doanh thu hom qua" -> REPORT

        RECENT CONVERSATION HISTORY (Use this context to resolve pronouns like "nó", "mã gì", "vậy"):
        {{history}}

        OUTPUT RULES (CRITICAL):
        - Return ONLY the label word. Nothing else.
        - No explanation. No punctuation. No code. No sentences.
        - Valid outputs: FINANCE | OPS | REPORT | GREET | OUT_OF_SCOPE
        """)
    String routeIntent(@dev.langchain4j.service.MemoryId String memoryId, @UserMessage String userMessage, @dev.langchain4j.service.V("history") String history);
}
