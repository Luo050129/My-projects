package com.luoxianjun.community.module.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发布帖子请求。
 * <p>
 * postType / visibility 允许缺省：缺省时按「纯文字 + 公开」处理，
 * 这样老客户端不改也能继续发帖。
 */
public record CreatePostRequest(
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

        /** TEXT 纯文字 / IMAGE 图文，为空按 TEXT */
        String postType,

        /** 0 公开 / 1 私密，为空按 0 */
        Integer visibility,

        /** 作者声明该内容由 AI 生成 / 辅助创作；缺省按 false，老客户端无需改动 */
        Boolean aiGenerated
) {
    /** 六参重载：保持既有调用方（含集成测试）兼容 */
    public CreatePostRequest(String title, String content, String tags, String coverImage,
                             String postType, Integer visibility) {
        this(title, content, tags, coverImage, postType, visibility, Boolean.FALSE);
    }
}
