package com.fnb.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tu dong khoi tao Knowledge Base bang file SQL tai thoi diem Spring Boot khoi dong.
 * [Phase 3.1] Setup F&B Knowledge Base.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseInitRunner {

    private final JdbcTemplate jdbcTemplate;
    private final dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @EventListener(ApplicationReadyEvent.class)
    public void initDatabase() {
        try {
            log.info("[DB-INIT] Kiem tra va khoi tao Knowledge Base...");
            ClassPathResource resource = new ClassPathResource("db/init_knowledge_base.sql");
            if (resource.exists()) {
                String sql = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));
                jdbcTemplate.execute(sql);
                log.info("[DB-INIT] Khoi tao Knowledge Base thanh cong.");
                
                embedKnowledgeBase();
            } else {
                log.warn("[DB-INIT] Khong tim thay file db/init_knowledge_base.sql");
            }
        } catch (Exception e) {
            log.error("[DB-INIT] Loi khoi tao database: {}", e.getMessage());
        }
    }

    private void embedKnowledgeBase() {
        try {
            String selectSql = "SELECT id, content FROM ai.knowledge_base WHERE embedding IS NULL";
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql);
            if (rows.isEmpty()) {
                log.info("[DB-INIT] Tat ca document trong Knowledge Base da duoc embed.");
                return;
            }

            log.info("[DB-INIT] Tien hanh embed {} documents...", rows.size());
            int count = 0;
            for (Map<String, Object> row : rows) {
                UUID id = (UUID) row.get("id");
                String content = (String) row.get("content");
                
                dev.langchain4j.data.embedding.Embedding embedding = embeddingModel.embed(content).content();
                String vectorStr = java.util.Arrays.toString(embedding.vector());
                
                String updateSql = "UPDATE ai.knowledge_base SET embedding = ?::vector WHERE id = ?";
                jdbcTemplate.update(updateSql, vectorStr, id);
                count++;
            }
            log.info("[DB-INIT] Da tao embedding xong cho {} documents.", count);
        } catch (Exception e) {
            log.error("[DB-INIT] Loi khi tao embedding cho Knowledge Base: {}", e.getMessage());
        }
    }
}
