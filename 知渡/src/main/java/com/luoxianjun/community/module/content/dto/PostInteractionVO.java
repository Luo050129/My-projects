package com.luoxianjun.community.module.content.dto;

/**
 * 当前用户与某帖的交互状态：详情页用来渲染「已赞 / 已收藏」高亮。
 * <p>
 * 单独成接口而不是并进 PostVO，是为了让帖子详情缓存保持与用户无关、可以被所有人复用。
 */
public record PostInteractionVO(boolean liked, boolean favorited) {
}
