package com.fnb.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.UUID;

/**
 * Cấu hình AI.
 * Quản lý bộ nhớ ngữ cảnh (Chat Memory) cho từng phiên chat.
 */
@Configuration
public class AiConfig {

    /**
     * Cung cấp ChatMemory cho mỗi @MemoryId.
     * Tích hợp Redis để lưu trữ hội thoại đồng bộ giữa các instance.
     */
    @Bean
    public ChatMemoryProvider chatMemoryProvider(RedisChatMemoryStore redisChatMemoryStore) {
        return memoryId -> MessageWindowChatMemory.builder()
                .id(memoryId)
                .maxMessages(10) // Tối ưu: Giữ 10 tin nhắn (~2 lượt hội thoại khứ hồi có gọi Tool) để tiết kiệm Token
                .chatMemoryStore(redisChatMemoryStore)
                .build();
    }

    @Bean
    public dev.langchain4j.model.embedding.EmbeddingModel embeddingModel() {
        return new dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel();
    }
}
