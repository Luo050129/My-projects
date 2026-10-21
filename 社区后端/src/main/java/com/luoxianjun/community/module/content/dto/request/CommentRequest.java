package com.luoxianjun.community.module.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发表评论请求。
 */
public record CommentRequest(
        @NotNull(message = "帖子ID不能为空")
        Long postId,

        @NotBlank(message = "评论内容不能为空")
        @Size(max = 500, message = "评论过长")
        String content,

        Long parentId
) {
}
