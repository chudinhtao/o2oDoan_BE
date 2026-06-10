package com.fnb.ai.config;

import com.fnb.ai.agent.SummarizerAgent;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class SummarizingChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(SummarizingChatMemory.class);

    private final Object id;
    private final ChatMemory delegate;
    private final SummarizerAgent summarizerAgent;
    private final int summarizeThreshold; // Ví dụ: 6 tin nhắn

    public SummarizingChatMemory(Object id, ChatMemoryStore store, SummarizerAgent summarizerAgent, int summarizeThreshold) {
        this.id = id;
        this.summarizerAgent = summarizerAgent;
        this.summarizeThreshold = summarizeThreshold;
        // Delegate dùng maxMessages lớn để không cắt ngang các tin nhắn ToolCall
        this.delegate = MessageWindowChatMemory.builder()
                .id(id)
                .maxMessages(summarizeThreshold + 20)
                .chatMemoryStore(store)
                .build();
    }

    @Override
    public Object id() {
        return id;
    }

    @Override
    public void add(ChatMessage message) {
        // Chỉ bắt đầu tóm tắt khi nhận được UserMessage mới (bắt đầu một lượt hội thoại mới)
        // Điều này đảm bảo ta không phá vỡ chuỗi FunctionCall -> FunctionResult của Gemini.
        if (message instanceof dev.langchain4j.data.message.UserMessage) {
            List<ChatMessage> currentMessages = delegate.messages();
            if (currentMessages.size() >= summarizeThreshold) {
                log.info("[SummarizingChatMemory] Đạt ngưỡng {} tin nhắn, bắt đầu tóm tắt cho bàn {}", currentMessages.size(), id);
                
                // Xây dựng nội dung để tóm tắt
                String history = currentMessages.stream()
                        .map(msg -> msg.type() + ": " + msg.text())
                        .collect(Collectors.joining("\n"));
                
                try {
                    // Gọi AI để tóm tắt
                    String summarizerMemoryId = "stateless-summarizer-" + java.util.UUID.randomUUID().toString();
                    String summary = summarizerAgent.summarize(summarizerMemoryId, history);
                    log.info("[SummarizingChatMemory] Kết quả tóm tắt: {}", summary);
                    
                    // Clear toàn bộ bộ nhớ cũ
                    delegate.clear();
                    
                    // Thêm câu tóm tắt như là một SystemMessage (ngữ cảnh)
                    delegate.add(new SystemMessage("Ngữ cảnh hội thoại trước đó (Tóm tắt): " + summary));
                    
                } catch (Exception e) {
                    log.error("[SummarizingChatMemory] Lỗi khi tóm tắt: {}", e.getMessage());
                }
            }
        }
        
        // Luôn add tin nhắn mới vào cuối
        delegate.add(message);
    }

    @Override
    public List<ChatMessage> messages() {
        return delegate.messages();
    }

    @Override
    public void clear() {
        delegate.clear();
    }
}
