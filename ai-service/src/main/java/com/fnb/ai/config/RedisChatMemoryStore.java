package com.fnb.ai.config;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lưu trữ ChatMemory vào Redis để chia sẻ context giữa nhiều instance
 * và không bị mất lịch sử khi server khởi động lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMemoryStore implements ChatMemoryStore {

    private final StringRedisTemplate redisTemplate;
    
    // Key prefix trong Redis
    private static final String PREFIX = "ai:chat_memory:";
    // TTL: Thời gian sống của bộ nhớ chat (vd: 2 giờ)
    private static final long TTL_HOURS = 2;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String key = PREFIX + memoryId;
        String json = redisTemplate.opsForValue().get(key);
        
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        
        try {
            return ChatMessageDeserializer.messagesFromJson(json);
        } catch (Exception e) {
            log.warn("[REDIS-MEMORY] Lỗi khi deserialize lịch sử chat cho id {}: {}", memoryId, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String key = PREFIX + memoryId;
        try {
            String json = ChatMessageSerializer.messagesToJson(messages);
            redisTemplate.opsForValue().set(key, json, TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("[REDIS-MEMORY] Lỗi khi lưu lịch sử chat cho id {}: {}", memoryId, e.getMessage());
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        String key = PREFIX + memoryId;
        redisTemplate.delete(key);
        log.debug("[REDIS-MEMORY] Xóa lịch sử chat: {}", memoryId);
    }
}
