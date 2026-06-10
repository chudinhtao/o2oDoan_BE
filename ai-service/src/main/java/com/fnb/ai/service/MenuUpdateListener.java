package com.fnb.ai.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuUpdateListener {

    private final JdbcTemplate jdbc;
    private final EmbeddingModel embeddingModel;

    public void handleMenuSync(String message) {
        log.info("[REDIS-LISTENER] Nhận yêu cầu đồng bộ Vector cho món ăn ID: {}", message);
        try {
            // Loại bỏ khoảng trắng hoặc ký tự thừa nếu có
            String itemId = message.trim().replace("\"", "");
            
            // 1. Query thông tin mới nhất từ DB
            String sql = "SELECT name, description FROM menu.menu_items WHERE id = ?::uuid";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, itemId);
            
            if (rows.isEmpty()) {
                log.warn("[REDIS-LISTENER] Không tìm thấy món ăn ID: {}. Có thể đã bị xóa.", itemId);
                return;
            }

            Map<String, Object> item = rows.get(0);
            String name = (String) item.get("name");
            String description = item.get("description") != null ? (String) item.get("description") : "";
            String textToEmbed = "Tên món: " + name + ". Mô tả: " + description;

            // 2. Tạo Vector
            Embedding embedding = embeddingModel.embed(textToEmbed).content();
            float[] vector = embedding.vector();
            String vectorStr = buildVectorString(vector);

            // 3. Update lại cột embedding
            jdbc.update("UPDATE menu.menu_items SET embedding = ?::vector WHERE id = ?::uuid", vectorStr, itemId);
            log.info("[REDIS-LISTENER] ✅ Đã cập nhật xong Vector cho món: {}", name);

        } catch (Exception e) {
            log.error("[REDIS-LISTENER] ❌ Lỗi khi đồng bộ Vector: {}", e.getMessage());
        }
    }

    private String buildVectorString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
