package com.luoxianjun.community.module.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoxianjun.community.module.ai.dto.DeepSeekDto;
import com.luoxianjun.community.module.ai.properties.DeepSeekProperties;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.PostService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * AI 内容增强单测（Mockito）：聚焦「无 Key 时规则降级」这一核心容错分支，
 * 不依赖外部大模型，离线可跑。
 */
class ContentAiServiceTest {

    private final DeepSeekProperties props = mock(DeepSeekProperties.class);
    private final PostMapper postMapper = mock(PostMapper.class);
    private final PostService postService = mock(PostService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ContentAiService service() {
        // deepSeekClient 在降级分支不会被调用，传 null 即可
        return new ContentAiService(props, null, postMapper, postService, objectMapper);
    }

    private Post samplePost(String content) {
        Post post = new Post();
        post.setId(1L);
        post.setTitle("测试标题");
        post.setContent(content);
        post.setDeleted(0);
        return post;
    }

    @Test
    void fallbackUsedWhenKeyNotConfigured() {
        when(props.isConfigured()).thenReturn(false);
        when(postMapper.selectById(1L)).thenReturn(samplePost("这是一段用于测试的内容，长度足够用来验证摘要截断逻辑是否正常工作。"));

        DeepSeekDto.AiResult result = service().enhancePost(1L);

        assertNotNull(result);
        assertEquals("社区,分享,随笔", result.tags());
        assertTrue(result.safe());
        // 降级结果应回写帖子
        verify(postService).applyAiEnhancement(eq(1L), anyString(), eq("社区,分享,随笔"));
    }

    @Test
    void bannedWordMarksUnsafe() {
        when(props.isConfigured()).thenReturn(false);
        when(postMapper.selectById(2L)).thenReturn(samplePost("这是一篇违规广告内容，请勿购买。"));

        DeepSeekDto.AiResult result = service().enhancePost(2L);

        assertFalse(result.safe());
    }

    @Test
    void longContentSummaryTruncated() {
        when(props.isConfigured()).thenReturn(false);
        String longContent = "内容".repeat(100);
        when(postMapper.selectById(3L)).thenReturn(samplePost(longContent));

        DeepSeekDto.AiResult result = service().enhancePost(3L);

        assertTrue(result.summary().length() <= 81, "摘要应被截断到 80 字+省略号");
    }
}
