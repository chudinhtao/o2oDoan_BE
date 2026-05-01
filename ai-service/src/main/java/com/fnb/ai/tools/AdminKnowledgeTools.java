package com.fnb.ai.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bo cong cu Tri thuc va Boi canh (Phase 3).
 * Cung cap:
 *   - searchKnowledgeBase : RAG tren CSDL F&B Benchmarks.
 *   - getWeatherAndEvents : Thong tin thoi tiet / su kien dia phuong (Mock).
 *   - getMarketTrends     : Xu huong thi truong hien tai (Mock Web Search).
 */
@Slf4j
@Component("adminKnowledgeTools")
@RequiredArgsConstructor
public class AdminKnowledgeTools {

    private final JdbcTemplate jdbc;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @Tool("Tim kiem cac tieu chuan, benchmark cua nganh F&B tu Knowledge Base (Vi du: ty le huy don an toan, food cost tieu chuan, ma tran Menu Engineering). " +
          "Dung khi can tham khao chuan muc nganh hoac ly thuyet van hanh nha hang de dua ra loi khuyen.")
    public String searchKnowledgeBase(@P("Truy van tim kiem, nen dung tieng Viet") String query) {
        log.info("[KNOWLEDGE-TOOL] searchKnowledgeBase: {}", query);
        try {
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(query).content();
            String vectorString = java.util.Arrays.toString(embedding.vector());

            String sql = """
                SELECT title, content
                FROM ai.knowledge_base
                WHERE embedding IS NOT NULL
                ORDER BY embedding <-> ?::vector
                LIMIT 3
                """;
            
            List<Map<String, Object>> rows = jdbc.queryForList(sql, vectorString);
            if (rows.isEmpty()) {
                return "Khong tim thay thong tin trong Knowledge Base.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📚 THONG TIN TU KNOWLEDGE BASE:\n");
            for (Map<String, Object> row : rows) {
                sb.append("• [").append(row.get("title")).append("] ")
                  .append(row.get("content")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[KNOWLEDGE-TOOL] searchKnowledgeBase error: {}", e.getMessage());
            return "Loi khi truy cap Knowledge Base. " + e.getMessage();
        }
    }

    @Tool("Lay thong tin thoi tiet va su kien dia phuong hom nay tai khu vuc cua nha hang. " +
          "Dung de danh gia nguyen nhan khach vang, luong don takeaway tang/giam hoac du doan doanh thu.")
    public String getWeatherAndEvents() {
        log.info("[KNOWLEDGE-TOOL] getWeatherAndEvents");
        return """
            🌤️ THOI TIET & SU KIEN HOM NAY (Khu vuc TP.HCM):
            - Thoi tiet: Mua to vao buoi chieu toi (16:00 - 19:00). Nhiet do 26-30 do C.
            - Su kien: Co tran chung ket bong da luc 19:30 toi nay.
            
            💡 Insights: Mua to co the lam giam luong khach an tai quan (Dine-in) nhung se lam tang dot bien luong don giao di (Takeaway/Delivery). Tran bong da vao buoi toi khuyen khich cac combo nhom hoac bia/do nham.
            """;
    }

    @Tool("Tim kiem xu huong nganh F&B tren mang (Web Search) de biet trend hien tai, mon an dang hot hoac thay doi trong thi truong. " +
          "Dung khi can de xuat mon moi, chuong trinh khuyen mai theo trend hoac danh gia xem nha hang co bi tut hau khong.")
    public String getMarketTrends() {
        log.info("[KNOWLEDGE-TOOL] getMarketTrends");
        return """
            📈 XU HUONG THI TRUONG F&B (Web Search Mock):
            1. "Healty & Diet": Nhu cau cac mon an Eat Clean, thuc uong it duong (Keto, low-carb) dang tang 25% so voi cung ky nam ngoai.
            2. "Combo tiet kiem": Do kinh te kho khan, khach hang chuong cac set lunch hoac combo giam gia cho nhom tu 2-4 nguoi.
            3. "Bao bi xanh": Ngay cang nhieu khach hang ung ho cac nha hang su dung hop giay, ong hut thien nhien thay vi nhua.
            """;
    }
}
