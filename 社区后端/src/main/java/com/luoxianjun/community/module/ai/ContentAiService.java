package com.luoxianjun.community.module.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.module.ai.dto.DeepSeekDto;
import com.luoxianjun.community.module.ai.properties.DeepSeekProperties;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 内容增强：为帖子生成摘要 / 标签 / 合规判定。
 * 未配置 Key 或调用失败时降级为规则实现，保证链路永远可跑。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentAiService {

    private static final String SYSTEM_PROMPT = """
            你是社区内容审核与摘要助手。请根据用户提供的标题与正文，输出严格 JSON，
            不要包含任何解释或 Markdown 代码块标记。格式：
            {"summary":"不超过80字的中文摘要","tags":"3个逗号分隔的中文标签","safe":true或false}
            """;

    private final DeepSeekProperties props;
    private final DeepSeekClient deepSeekClient;
    private final PostMapper postMapper;
    private final PostService postService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DeepSeekDto.AiResult enhancePost(Long postId) {
        Post post = postMapper.selectById(postId);
        // deleted 为逻辑删除标记；DB 默认 0，这里做空值防御，避免未持久化对象拆箱 NPE
        if (post == null || (post.getDeleted() != null && post.getDeleted() == 1)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        DeepSeekDto.AiResult result = props.isConfigured()
                ? callDeepSeek(post)
                : ruleBased(post);
        if (result == null) {
            result = ruleBased(post);
        }
        postService.applyAiEnhancement(postId, result.summary(), result.tags());
        return result;
    }

    private DeepSeekDto.AiResult callDeepSeek(Post post) {
        String user = "标题：" + post.getTitle() + "\n正文：" + post.getContent();
        String raw = deepSeekClient.chat(SYSTEM_PROMPT, user);
        if (raw == null) {
            return null;
        }
        try {
            String json = extractJson(raw);
            AiJson parsed = objectMapper.readValue(json, AiJson.class);
            return new DeepSeekDto.AiResult(
                    parsed.summary() == null ? "" : parsed.summary(),
                    parsed.tags() == null ? "" : parsed.tags(),
                    parsed.safe() != null && parsed.safe());
        } catch (Exception e) {
            log.warn("解析 DeepSeek 返回失败，降级处理: {}", e.getMessage());
            return null;
        }
    }

    /** 从模型可能附带的 ```json 围栏中提取纯 JSON */
    private String extractJson(String raw) {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return raw;
    }

    /** 规则降级：不依赖外部模型，离线可跑 */
    private DeepSeekDto.AiResult ruleBased(Post post) {
        String plain = post.getContent().replaceAll("\\s+", " ");
        String summary = plain.length() <= 80 ? plain : plain.substring(0, 80) + "…";
        boolean safe = !containsBannedWords(post.getTitle() + post.getContent());
        return new DeepSeekDto.AiResult(summary, "社区,分享,随笔", safe);
    }

    private boolean containsBannedWords(String text) {
        String[] banned = {"违规", "广告", "赌博", "色情"};
        for (String w : banned) {
            if (text.contains(w)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AiJson(String summary, String tags, Boolean safe) {
    }
}
