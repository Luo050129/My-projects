package com.luoxianjun.community.module.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发布帖子请求。
 */
public record CreatePostRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题过长")
        String title,

        @NotBlank(message = "正文不能为空")
        @Size(max = 8000, message = "正文过长")
        String content,

        @Size(max = 255, message = "标签过长")
        String tags
) {
}
