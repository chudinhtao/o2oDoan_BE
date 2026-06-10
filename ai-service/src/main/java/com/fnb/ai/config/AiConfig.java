package com.fnb.ai.config;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Cấu hình AI Multi-Model Architecture.
 *
 * - fastChatModel: Groq (siêu nhanh) → Gemini Flash (fallback)
 *   Dùng cho: QueryRewriterAgent, CustomerRouterAgent, SummarizerAgent
 *
 * - smartChatModel: Gemini (thông minh)
 *   Dùng cho: MenuAgent, OrderAgent, GeneralAgent, Admin Agents
 */
@Slf4j
@Configuration
public class AiConfig {

    // ═══════════════════════════════════════════════════════════════
    // 1. FAST MODEL (Groq + Gemini Fallback)
    //    Cho tác vụ nhẹ: Rewrite, Route, Summarize
    // ═══════════════════════════════════════════════════════════════

    @Bean("fastChatModel")
    public ChatLanguageModel fastChatModel(
            @Value("${ai.groq.api-key:}") String groqApiKey,
            @Value("${ai.groq.base-url:https://api.groq.com/openai/v1}") String groqBaseUrl,
            @Value("${ai.groq.model-name:llama-3.1-8b-instant}") String groqModel,
            @Value("${langchain4j.open-ai.chat-model.api-key}") String geminiApiKey) {

        // Nếu có Groq API Key, dùng Groq làm primary với Gemini làm fallback
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            ChatLanguageModel groqModel_ = OpenAiChatModel.builder()
                    .baseUrl(groqBaseUrl)
                    .apiKey(groqApiKey)
                    .modelName(groqModel)
                    .temperature(0.0)
                    .maxTokens(512)
                    .build();

            ChatLanguageModel geminiFallback = GoogleAiGeminiChatModel.builder()
                    .apiKey(geminiApiKey)
                    .modelName("gemini-3.1-flash-lite")
                    .temperature(0.0)
                    .build();

            log.info("[AI-CONFIG] FastModel: Groq ({}) + Gemini Fallback", groqModel);
            return new FallbackChatModel(groqModel_, geminiFallback);
        }

        // Nếu không có Groq Key, dùng Gemini trực tiếp
        log.warn("[AI-CONFIG] GROQ_API_KEY chưa được cấu hình. FastModel sẽ dùng Gemini.");
        return GoogleAiGeminiChatModel.builder()
                .apiKey(geminiApiKey)
                .modelName("gemini-3.1-flash-lite")
                .temperature(0.0)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // 2. SMART MODEL (Gemini chính)
    //    Cho tác vụ nặng: Tư vấn, Phân tích báo cáo
    // ═══════════════════════════════════════════════════════════════

    @Bean("smartChatModel")
    public ChatLanguageModel smartChatModel(
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.model-name}") String modelName) {
        log.info("[AI-CONFIG] SmartModel: Gemini ({})", modelName);
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.0)
                .build();
    }

    // ═══════════════════════════════════════════════════════════════
    // 3. CHAT MEMORY (Giữ nguyên logic cũ)
    // ═══════════════════════════════════════════════════════════════

    @Bean
    public ChatMemoryProvider chatMemoryProvider(
            RedisChatMemoryStore redisChatMemoryStore,
            @Lazy com.fnb.ai.agent.SummarizerAgent summarizerAgent) {
        return memoryId -> {
            String id = memoryId.toString();
            if ("default".equals(id) || id.startsWith("stateless-")) {
                return MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .build();
            }
            return new SummarizingChatMemory(memoryId, redisChatMemoryStore, summarizerAgent, 6);
        };
    }

    @Bean
    public dev.langchain4j.model.embedding.EmbeddingModel embeddingModel() {
        return new dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel();
    }

    // ═══════════════════════════════════════════════════════════════
    // Inner class: Fallback Model wrapper đơn giản
    // Nếu primary lỗi (Rate Limit 429, timeout...) -> Tự động chuyển sang fallback
    // ═══════════════════════════════════════════════════════════════

    private static class FallbackChatModel implements ChatLanguageModel {
        private final ChatLanguageModel primary;
        private final ChatLanguageModel fallback;

        FallbackChatModel(ChatLanguageModel primary, ChatLanguageModel fallback) {
            this.primary = primary;
            this.fallback = fallback;
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(java.util.List<dev.langchain4j.data.message.ChatMessage> messages) {
            try {
                return primary.generate(messages);
            } catch (Exception e) {
                log.warn("[FALLBACK] Primary model lỗi ({}), chuyển sang Fallback: {}", e.getClass().getSimpleName(), e.getMessage());
                return fallback.generate(messages);
            }
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                java.util.List<dev.langchain4j.agent.tool.ToolSpecification> toolSpecifications) {
            try {
                return primary.generate(messages, toolSpecifications);
            } catch (Exception e) {
                log.warn("[FALLBACK] Primary model lỗi ({}), chuyển sang Fallback: {}", e.getClass().getSimpleName(), e.getMessage());
                return fallback.generate(messages, toolSpecifications);
            }
        }

        @Override
        public dev.langchain4j.model.output.Response<dev.langchain4j.data.message.AiMessage> generate(
                java.util.List<dev.langchain4j.data.message.ChatMessage> messages,
                dev.langchain4j.agent.tool.ToolSpecification toolSpecification) {
            try {
                return primary.generate(messages, toolSpecification);
            } catch (Exception e) {
                log.warn("[FALLBACK] Primary model lỗi ({}), chuyển sang Fallback: {}", e.getClass().getSimpleName(), e.getMessage());
                return fallback.generate(messages, toolSpecification);
            }
        }

        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FallbackChatModel.class);
    }
}
