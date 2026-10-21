package com.luoxianjun.community.module.ai;

import com.luoxianjun.community.module.ai.dto.DeepSeekDto;
import com.luoxianjun.community.module.ai.properties.DeepSeekProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 自研轻量 DeepSeek 客户端：基于 OpenAI 兼容协议，用 RestClient 直连。
 * 不依赖任何 SDK，便于切换模型 / 控制成本 / 统一错误处理。
 */
@Slf4j
@Component
public class DeepSeekClient {

    private final RestClient client;
    private final DeepSeekProperties props;

    public DeepSeekClient(DeepSeekProperties props, RestClient.Builder builder) {
        this.props = props;
        this.client = builder
                .baseUrl(props.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + props.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * 单次对话，返回助手文本；失败返回 null（交由上层降级）。
     */
    public String chat(String systemPrompt, String userPrompt) {
        DeepSeekDto.Request body = new DeepSeekDto.Request(
                props.getModel(),
                List.of(
                        new DeepSeekDto.Message("system", systemPrompt),
                        new DeepSeekDto.Message("user", userPrompt)),
                false);
        try {
            DeepSeekDto.Response resp = client.post()
                    .uri("/v1/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(DeepSeekDto.Response.class);
            if (resp != null && resp.choices() != null && !resp.choices().isEmpty()) {
                return resp.choices().get(0).message().content();
            }
            return null;
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败: {}", e.getMessage());
            return null;
        }
    }
}
