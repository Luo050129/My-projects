package com.luoxianjun.community.module.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置（对应 application.yml 中 community.ai.*）。
 * api-key 优先读环境变量 DEEPSEEK_API_KEY，留空则启用规则降级实现。
 */
@Data
@Component
@ConfigurationProperties(prefix = "community.ai")
public class DeepSeekProperties {
    private boolean enabled = true;
    private String provider = "deepseek";
    private String baseUrl = "https://api.deepseek.com";
    private String apiKey = "";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 20;

    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
