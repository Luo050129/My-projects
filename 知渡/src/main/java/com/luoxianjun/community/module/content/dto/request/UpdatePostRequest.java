package com.luoxianjun.community.module.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新帖子请求。允许在编辑时调整形态与可见性（例如把公开帖改为私密）。
 */
public record UpdatePostRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题过长")
        String title,

        @NotBlank(message = "正文不能为空")
        @Size(max = 8000, message = "正文过长")
        String content,

        @Size(max = 255, message = "标签过长")
        String tags,

        @Size(max = 255, message = "封面图路径过长")
        String coverImage,

        String postType,

        Integer visibility
) {
}
