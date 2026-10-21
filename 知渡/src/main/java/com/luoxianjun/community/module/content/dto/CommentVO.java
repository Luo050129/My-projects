package com.luoxianjun.community.module.content.dto;

import java.time.LocalDateTime;

/**
 * 评论视图对象。
 */
public record CommentVO(
        Long id,
        Long postId,
        Long userId,
        String authorName,
        Long parentId,
        String content,
        LocalDateTime createTime
) {
}
