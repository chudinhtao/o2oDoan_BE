package com.fnb.common.config;

import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration cho common-core.
 * InternalSecretFilter được đăng ký trực tiếp trong từng service's SecurityConfig
 * để tránh duplicate bean và kiểm soát thứ tự filter tốt hơn.
 */
@Configuration
public class CommonCoreAutoConfiguration {
    // Intentionally empty — beans are registered per-service in their SecurityConfig
}
