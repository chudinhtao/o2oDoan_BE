package com.fnb.ai.controller;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * API nội bộ để đồng bộ Vector Embedding cho toàn bộ món ăn.
 * Chạy 1 lần sau khi deploy hoặc khi có lượng lớn dữ liệu mới.
 *
 * Gọi: POST http://localhost:8087/api/internal/ai/sync-menu-vectors
 * KHÔNG expose qua api-gateway (chỉ dùng internal).
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/ai")
@RequiredArgsConstructor
public class VectorSyncController {

    private final EmbeddingModel embeddingModel;
    private final JdbcTemplate jdbc;

    @PostMapping("/sync-menu-vectors")
    public String syncMenuVectors() {
        log.info("[VECTOR-SYNC] Bắt đầu đồng bộ embedding cho menu items...");

        // Lấy các món chưa có embedding
        List<Map<String, Object>> items = jdbc.queryForList(
                "SELECT id, name, description FROM menu.menu_items WHERE embedding IS NULL AND is_active = true"
        );

        if (items.isEmpty()) {
            return "✅ Tất cả món ăn đã có embedding. Không cần đồng bộ.";
        }

        int successCount = 0;
        int failCount = 0;

        for (Map<String, Object> item : items) {
            try {
                String name = (String) item.get("name");
                String description = item.get("description") != null ? (String) item.get("description") : "";
                String textToEmbed = "Tên món: " + name + ". Mô tả: " + description;

                // Tạo vector từ text
                Embedding embedding = embeddingModel.embed(textToEmbed).content();
                float[] vector = embedding.vector();

                // Tạo string dạng '[0.1, 0.2, ...]' để CAST sang vector
                String vectorStr = buildVectorString(vector);

                jdbc.update(
                        "UPDATE menu.menu_items SET embedding = ?::vector WHERE id = ?::uuid",
                        vectorStr,
                        item.get("id").toString()
                );

                successCount++;
                log.debug("[VECTOR-SYNC] ✅ Đồng bộ thành công: {}", name);

            } catch (Exception e) {
                failCount++;
                log.error("[VECTOR-SYNC] ❌ Lỗi khi đồng bộ item {}: {}", item.get("id"), e.getMessage());
            }
        }

        String result = String.format(
                "✅ Đồng bộ hoàn tất! Thành công: %d / %d. Thất bại: %d.",
                successCount, items.size(), failCount
        );
        log.info("[VECTOR-SYNC] {}", result);
        return result;
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
