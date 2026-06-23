package com.fnb.ai.agent.customer;

import com.fnb.ai.config.RedisChatMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import com.fnb.ai.tools.CustomerAiTools;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Orchestrator điều phối luồng chat của Customer.
 *
 * Luồng xử lý:
 * 1. Hybrid Routing (Regex) cho lệnh đơn giản (tính tiền, dọn bàn...)
 * 2. LLM Router phân loại ý định (MENU / ORDER / GENERAL)
 * 3. [MỚI] Query Rewriter: Viết lại câu hỏi thành câu độc lập
 * 4. [MỚI] Global Semantic Cache: Tra cứu Vector không phụ thuộc session
 * 5. Gọi Agent chuyên biệt (MenuAgent / OrderAgent / GeneralAgent)
 * 6. Lưu kết quả vào Global Cache
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerOrchestrator {

    private final CustomerRouterAgent routerAgent;
    private final MenuAgent menuAgent;
    private final OrderAgent orderAgent;
    private final GeneralAgent generalAgent;
    private final QueryRewriterAgent queryRewriterAgent;
    private final CustomerAiTools tools;
    private final JdbcTemplate jdbc;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;
    private final RedisChatMemoryStore chatMemoryStore;

    public String processChat(String sessionToken, String userMessage) {
        String msgLower = userMessage.toLowerCase().trim();
        String msgUnaccented = removeAccents(msgLower);

        // --- CHIẾN LƯỢC 1: HYBRID ROUTING (Bỏ qua LLM cho lệnh đơn giản) ---
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

        // --- CHIẾN LƯỢC 2: LLM ROUTING ---
        String routerMemoryId = "stateless-router-" + UUID.randomUUID().toString();
        String intent = routerAgent.routeIntent(routerMemoryId, userMessage).trim().toUpperCase();
        log.debug("[ORCHESTRATOR] sessionToken={} | intent={} | msg={}", sessionToken, intent, userMessage);

        String tableInfo = "Session: " + sessionToken + " (Chưa xác định bàn)";
        try {
            String tableSql = "SELECT t.name, t.zone FROM orders.table_sessions ts JOIN orders.tables t ON ts.table_id = t.id WHERE ts.session_token = ?";
            List<Map<String, Object>> rows = jdbc.queryForList(tableSql, sessionToken);
            if (!rows.isEmpty()) {
                String tName = (String) rows.get(0).get("name");
                String tZone = (String) rows.get(0).get("zone");
                tableInfo = String.format("Bàn: %s (Khu vực: %s) - Session: %s", 
                                tName != null ? tName : "Chưa đặt tên", 
                                tZone != null ? tZone : "Chung", 
                                sessionToken);
            }
        } catch (Exception e) {
            log.warn("[ORCHESTRATOR] Lỗi lấy thông tin bàn: {}", e.getMessage());
        }

        if (intent.contains("ORDER")) {
            return orderAgent.chat(sessionToken, userMessage, tableInfo);
        }

        if (intent.contains("GENERAL") || (!intent.contains("MENU") && !intent.contains("ORDER"))) {
            return generalAgent.chat(sessionToken, userMessage, tableInfo);
        }

        // --- CHIẾN LƯỢC 3: QUERY REWRITER + GLOBAL SEMANTIC CACHE (Chỉ cho MENU) ---
        String vectorString = null;
        String finalQueryForCache = userMessage;

        try {
            // 3.1: Kéo lịch sử chat và Viết lại câu hỏi thành câu Độc lập
            String rewrittenQuery = rewriteQuery(sessionToken, userMessage);
            log.info("[REWRITER] Gốc: '{}' -> Mới: '{}'", userMessage, rewrittenQuery);

            // Nếu là lệnh hành động -> Bỏ qua Cache, giao thẳng cho Agent
            if ("[ACTION_REQUIRED]".equals(rewrittenQuery)) {
                log.info("[REWRITER] Phát hiện lệnh hành động, bỏ qua Cache");
            } else {
                finalQueryForCache = rewrittenQuery;

                // 3.2: Embedding câu đã Rewrite và Tra cứu Global Cache
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(finalQueryForCache).content();
                vectorString = Arrays.toString(embedding.vector());

                String cacheQuery = "SELECT answer FROM menu.ai_semantic_cache " +
                                    "WHERE (embedding <=> ?::vector) < 0.02 " +
                                    "AND created_at > NOW() - INTERVAL '1 hour' " +
                                    "ORDER BY (embedding <=> ?::vector) ASC LIMIT 1";

                List<String> cached = jdbc.queryForList(cacheQuery, String.class, vectorString, vectorString);

                if (!cached.isEmpty()) {
                    log.info("[SEMANTIC CACHE] ⚡ Global Cache HIT cho: {}", finalQueryForCache);
                    return cached.get(0) + "\n\n*(⚡ Trả lời siêu tốc từ Bộ nhớ đệm AI)*";
                }
                log.info("[SEMANTIC CACHE] Global Cache MISS. Gọi LLM...");
            }
        } catch (Exception e) {
            log.warn("[SEMANTIC CACHE] Lỗi Rewrite/Cache, fallback về LLM: {}", e.getMessage());
        }

        // 3.3: Gọi LLM (vẫn dùng câu gốc để AI nói chuyện tự nhiên dựa vào Chat Memory)
        String aiResponse = menuAgent.chat(sessionToken, userMessage, tableInfo);

        // 3.4: Lưu vào Global Cache (session_token = NULL cho tất cả)
        if (vectorString != null) {
            try {
                String insertCache = "INSERT INTO menu.ai_semantic_cache (question, embedding, answer, session_token) VALUES (?, ?::vector, ?, NULL)";
                jdbc.update(insertCache, finalQueryForCache, vectorString, aiResponse);
                log.info("[SEMANTIC CACHE] Đã lưu Global Cache cho: {}", finalQueryForCache);
            } catch (Exception e) {
                log.warn("[SEMANTIC CACHE] Lỗi Insert Db: {}", e.getMessage());
            }
        }

        return aiResponse;
    }

    /**
     * Kéo 4 tin nhắn gần nhất từ Redis và gọi QueryRewriterAgent
     * để viết lại câu hỏi phụ thuộc ngữ cảnh thành câu Độc lập.
     */
    private String rewriteQuery(String sessionToken, String userMessage) {
        try {
            List<ChatMessage> messages = chatMemoryStore.getMessages(sessionToken);
            if (messages == null || messages.isEmpty()) {
                return userMessage; // Không có lịch sử -> Giữ nguyên
            }

            // Lấy tối đa 4 tin nhắn gần nhất
            int start = Math.max(0, messages.size() - 4);
            String history = messages.subList(start, messages.size()).stream()
                    .map(msg -> msg.type() + ": " + msg.text())
                    .collect(Collectors.joining("\n"));

            String rewriterMemoryId = "stateless-rewriter-" + UUID.randomUUID().toString();
            return queryRewriterAgent.rewrite(history, userMessage);
        } catch (Exception e) {
            log.warn("[REWRITER] Lỗi khi rewrite, giữ nguyên câu gốc: {}", e.getMessage());
            return userMessage;
        }
    }

    /**
     * Helper: Xóa dấu tiếng Việt để dễ dàng match Regex (vd: "tính tiền" -> "tinh tien")
     */
    private String removeAccents(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized)
                .replaceAll("").replace("đ", "d").replace("Đ", "D");
    }
}
