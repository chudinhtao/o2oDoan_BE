package com.fnb.ai.agent.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Orchestrator điều phối luồng chat của Customer.
 * 1. Router phân loại ý định (MENU / ORDER)
 * 2. Chuyển đến đúng Agent xử lý
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrchestrator {

    private final CustomerRouterAgent routerAgent;
    private final MenuAgent menuAgent;
    private final OrderAgent orderAgent;
    private final GeneralAgent generalAgent;
    private final com.fnb.ai.tools.CustomerAiTools tools;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    public String processChat(String sessionToken, String userMessage) {
        String msgLower = userMessage.toLowerCase().trim();
        String msgUnaccented = removeAccents(msgLower);

        // --- TỐI ƯU CHIẾN LƯỢC 3: HYBRID ROUTING (Bỏ qua LLM cho lệnh đơn giản) ---
        // Sử dụng chuỗi không dấu (msgUnaccented) để bắt được cả "tính tiền" lẫn "tinh tien"
        if (msgUnaccented.matches(".*(tinh tien|thanh toan|goi bill|tong ket).*") && msgLower.length() < 50) {
            log.info("[HYBRID ROUTING] Bypassed LLM for BILL intent");
            return tools.callStaff("BILL", "Khách yêu cầu tính tiền/thanh toán");
        }
        if (msgUnaccented.matches(".*(don ban|lau ban|don dep).*") && msgLower.length() < 50) {
            log.info("[HYBRID ROUTING] Bypassed LLM for CLEAN intent");
            return tools.callStaff("CLEAN", "Khách yêu cầu dọn bàn");
        }
        if (msgUnaccented.matches(".*(nuoc loc|lay da|them da|cham nuoc|lay giay|xin da|xin giay).*") && msgLower.length() < 50) {
            log.info("[HYBRID ROUTING] Bypassed LLM for WATER intent");
            return tools.callStaff("WATER", "Khách xin thêm nước/đá/giấy");
        }
        if (msgUnaccented.matches("^(goi phuc vu|nhan vien oi|em oi|ho tro|cho hoi).*") && msgLower.length() < 40) {
            log.info("[HYBRID ROUTING] Bypassed LLM for SUPPORT intent");
            return tools.callStaff("SUPPORT", "Khách gọi nhân viên hỗ trợ chung");
        }

        // --- GIAO CHO LLM NẾU CÂU HỎI PHỨC TẠP ---
        String intent = routerAgent.routeIntent(userMessage).trim().toUpperCase();
        log.debug("[ORCHESTRATOR] sessionToken={} | intent={} | msg={}", sessionToken, intent, userMessage);

        if (intent.contains("ORDER")) {
            return orderAgent.chat(sessionToken, userMessage, sessionToken);
        }

        if (intent.contains("GENERAL") || (!intent.contains("MENU") && !intent.contains("ORDER"))) {
            return generalAgent.chat(sessionToken, userMessage, sessionToken);
        }

        // --- TỐI ƯU CHIẾN LƯỢC 1: SEMANTIC CACHING CHO MENU ---
        // Chỉ áp dụng cache cho các câu hỏi chung chung (dài hơn 15 ký tự)
        if (userMessage.length() > 15) {
            try {
                // Mã hóa câu hỏi thành Vector
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(userMessage).content();
                String vectorString = java.util.Arrays.toString(embedding.vector());
                
                // Tìm trong Cache xem có câu nào giống >= 95% không VÀ chưa quá 15 phút
                String cacheQuery = "SELECT answer FROM menu.ai_semantic_cache WHERE embedding <=> ?::vector < 0.05 AND created_at > NOW() - INTERVAL '15 minutes' LIMIT 1";
                java.util.List<String> cached = jdbc.queryForList(cacheQuery, String.class, vectorString);
                
                if (!cached.isEmpty()) {
                    log.info("[SEMANTIC CACHE] ⚡ Cache HIT cho câu hỏi: {}", userMessage);
                    return cached.get(0) + "\n\n*(⚡ Trả lời siêu tốc từ Bộ nhớ đệm AI)*";
                }
                
                // Nếu Cache MISS -> Gọi LLM suy nghĩ
                log.info("[SEMANTIC CACHE] Cache MISS. Gọi LLM...");
                String aiResponse = menuAgent.chat(sessionToken, userMessage, sessionToken);
                
                // Lưu kết quả vào Cache để lần sau dùng
                String insertCache = "INSERT INTO menu.ai_semantic_cache (question, embedding, answer) VALUES (?, ?::vector, ?)";
                jdbc.update(insertCache, userMessage, vectorString, aiResponse);
                
                return aiResponse;
            } catch (Exception e) {
                log.warn("[SEMANTIC CACHE] Lỗi Cache, fallback về LLM: {}", e.getMessage());
            }
        }

        // Default: MENU (Không dùng Cache)
        return menuAgent.chat(sessionToken, userMessage, sessionToken);
    }

    /**
     * Helper: Xóa dấu tiếng Việt để dễ dàng match Regex (vd: "tính tiền" -> "tinh tien")
     */
    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD);
        return java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized)
                .replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}
