package com.luoxianjun.community.module.user.dto.response;

/**
 * 用户摘要：粉丝 / 关注列表、作者信息展示用。
 * <p>
 * 只暴露公开字段，绝不带 email、密码、状态等敏感信息。
 */
public record UserSummaryVO(
        Long id,
        String nickname,
        String role,
        long postCount,
        long followerCount,
        long followeeCount,
        /** 当前登录用户是否已关注 TA；未登录恒为 false */
        boolean following
) {
}
