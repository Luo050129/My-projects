package com.luoxianjun.community.module.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DeepSeek（OpenAI 兼容）请求/响应载体。
 */
public final class DeepSeekDto {

    private DeepSeekDto() {
    }

    public record Request(String model, List<Message> messages, boolean stream) {
    }

    public record Message(String role, String content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {
    }

    /** AI 返回的结构化结果 */
    public record AiResult(String summary, String tags, boolean safe) {
    }
}
