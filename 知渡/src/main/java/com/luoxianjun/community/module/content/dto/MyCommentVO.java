package com.luoxianjun.community.module.content.dto;

import java.time.LocalDateTime;

/**
 * 我的评论视图：带上所属帖子标题，省去前端二次查询。
 */
public record MyCommentVO(
        Long id,
        Long postId,
        String postTitle,
        String content,
        LocalDateTime createTime
) {
}
