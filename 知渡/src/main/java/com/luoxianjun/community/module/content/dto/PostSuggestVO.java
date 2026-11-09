package com.luoxianjun.community.module.content.dto;

/**
 * 搜索联想项：输入过程中下拉展示的轻量结果，按点赞数降序。
 *
 * @param snippet     命中词附近的上下文（来自摘要 / 正文 / 标签，标题本身已在 title 中展示）
 * @param matchedWord 本次命中的词，供前端标注「为什么弹出这条」
 */
public record PostSuggestVO(
        Long id,
        String title,
        String snippet,
        String authorName,
        Integer likeCount,
        Integer commentCount,
        String matchedWord
) {
}
