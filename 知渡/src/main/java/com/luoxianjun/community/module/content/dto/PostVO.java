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
        String tags,
        Integer status,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Integer aiEnhanced,
        LocalDateTime createTime
) {
}
