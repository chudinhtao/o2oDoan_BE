package com.fnb.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fnb.common.dto.ApiResponse;
import com.fnb.common.exception.BusinessException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Slf4j
@Configuration
public class FeignErrorDecoder implements ErrorDecoder {

    private final ErrorDecoder defaultErrorDecoder = new Default();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        try {
            if (response.body() != null) {
                try (InputStream inputStream = response.body().asInputStream()) {
                    com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(inputStream);
                    if (jsonNode != null && jsonNode.has("message") && !jsonNode.get("message").isNull()) {
                        String message = jsonNode.get("message").asText();
                        log.warn("Feign call failed, extracted message: {}", message);
                        return new BusinessException(message);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error decoding Feign exception response: {}", e.getMessage());
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}
