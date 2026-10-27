package com.luoxianjun.community.module.content.dto;

import java.time.LocalDateTime;

/**
 * 帖子视图对象。
 */
public record PostVO(
        Long id,
        Long authorId,
        String authorName,
        String title,
        String summary,
        /** 正文：仅详情接口返回；列表接口置 null，避免响应体过大 */
        String content,
        String tags,
        Integer status,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Integer aiEnhanced,
        LocalDateTime createTime
) {
}
