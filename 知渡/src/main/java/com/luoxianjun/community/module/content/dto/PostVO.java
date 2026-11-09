package com.luoxianjun.community.module.content.dto;

import java.time.LocalDateTime;

/**
 * 帖子视图对象。
 * <p>
 * 不含「当前用户是否点过赞 / 收藏过」这类用户态字段：详情会被缓存，
 * 缓存里塞用户态会串号。用户态走 {@code GET /posts/{id}/interaction} 单独查。
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
        /** 封面图访问路径，可为空 */
        String coverImage,
        /** TEXT 纯文字 / IMAGE 图文 */
        String postType,
        /** 0 公开 / 1 私密 */
        Integer visibility,
        Integer status,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        Integer aiEnhanced,
        /** 作者声明 AI 生成 / 辅助创作（显式标识，前端据此展示徽标） */
        Boolean aiGenerated,
        /** 审核状态：0 免审 / 1 待审 / 2 通过 / 3 驳回 */
        Integer reviewStatus,
        LocalDateTime createTime
) {
}
