package com.fnb.ai.config;

import com.fnb.ai.service.MenuUpdateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisPubSubConfig {

    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Đăng ký lắng nghe kênh "ai-menu-sync-topic"
        container.addMessageListener(listenerAdapter, new PatternTopic("ai-menu-sync-topic"));
        return container;
    }

    @Bean
    MessageListenerAdapter listenerAdapter(MenuUpdateListener listener) {
        // Chỉ định method "handleMenuSync" sẽ được gọi khi có Message tới
        return new MessageListenerAdapter(listener, "handleMenuSync");
    }
}
