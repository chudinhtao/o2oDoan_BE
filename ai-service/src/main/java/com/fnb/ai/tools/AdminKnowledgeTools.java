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
import java.util.Arrays;

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

    @Tool("Tìm kiếm các tiêu chuẩn, benchmark của ngành F&B từ Knowledge Base (Ví dụ: tỷ lệ hủy đơn an toàn, food cost tiêu chuẩn, ma trận Menu Engineering). " +
          "Dùng khi cần tham khảo chuẩn mực ngành hoặc lý thuyết vận hành nhà hàng để đưa ra lời khuyên.")
    public String searchKnowledgeBase(@P("Truy cập tìm kiếm, nên dùng tiếng Việt") String query) {
        log.info("[KNOWLEDGE-TOOL] searchKnowledgeBase: {}", query);
        if (query == null || query.trim().isEmpty()) {
            return "Vui lòng cung cấp từ khóa để tìm kiếm trong Knowledge Base.";
        }
        try {
            dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(query).content();
            String vectorString = Arrays.toString(embedding.vector());

            String sql = """
                SELECT title, content
                FROM ai.knowledge_base
                WHERE embedding IS NOT NULL
                ORDER BY embedding <-> ?::vector
                LIMIT 3
                """;
            
            List<Map<String, Object>> rows = jdbc.queryForList(sql, vectorString);
            if (rows.isEmpty()) {
                return "Không tìm thấy thông tin trong Knowledge Base.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📚 THÔNG TIN TỪ KNOWLEDGE BASE:\n");
            for (Map<String, Object> row : rows) {
                sb.append("• [").append(row.get("title")).append("] ")
                  .append(row.get("content")).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("[KNOWLEDGE-TOOL] searchKnowledgeBase error: {}", e.getMessage());
            return "Lỗi khi truy cập Knowledge Base. " + e.getMessage();
        }
    }

    @Tool("Lấy thông tin thời tiết và sự kiện địa phương hôm nay tại khu vực của nhà hàng. " +
          "Dùng để đánh giá nguyên nhân khách vắng, lượng đơn takeaway tăng/giảm hoặc dự đoán doanh thu.")
    public String getWeatherAndEvents() {
        log.info("[KNOWLEDGE-TOOL] getWeatherAndEvents");
        return """
            🌤️ THỜI TIẾT & SỰ KIỆN HÔM NAY (Khu vực TP.HCM):
            - Thời tiết: Mưa to vào buổi chiều tối (16:00 - 19:00). Nhiệt độ 26-30 độ C.
            - Sự kiện: Có trận chung kết bóng đá lúc 19:30 tối nay.
            
            💡 Insights: Mưa to có thể làm giảm lượng khách ăn tại quán (Dine-in) nhưng sẽ làm tăng đột biến lượng đơn giao đi (Takeaway/Delivery). Trận bóng đá vào buổi tối khuyến khích các combo nhóm hoặc bia/đồ nhắm.
            """;
    }

    @Tool("Tìm kiếm xu hướng ngành F&B trên mạng (Web Search) để biết trend hiện tại, món ăn đang hot hoặc thay đổi trong thị trường. " +
          "Dùng khi cần đề xuất món mới, chương trình khuyến mãi theo trend hoặc đánh giá xem nhà hàng có bị tụt hậu không.")
    public String getMarketTrends() {
        log.info("[KNOWLEDGE-TOOL] getMarketTrends");
        return """
            📈 XU HƯỚNG THỊ TRƯỜNG F&B (Web Search Mock):
            1. "Healthy & Diet": Nhu cầu các món ăn Eat Clean, thức uống ít đường (Keto, low-carb) đang tăng 25% so với cùng kỳ năm ngoái.
            2. "Combo tiết kiệm": Do kinh tế khó khăn, khách hàng chuộng các set lunch hoặc combo giảm giá cho nhóm từ 2-4 người.
            3. "Bao bì xanh": Ngày càng nhiều khách hàng ủng hộ các nhà hàng sử dụng hộp giấy, ống hút thiên nhiên thay vì nhựa.
            """;
    }
}
