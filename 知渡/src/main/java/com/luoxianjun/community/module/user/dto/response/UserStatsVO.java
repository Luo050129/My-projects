package com.luoxianjun.community.module.user.dto.response;

/**
 * 个人中心统计。一次查齐，避免前端为几个数字发四五个请求。
 */
public record UserStatsVO(
        /** 我发布的帖子数（含私密） */
        long postCount,
        /** 其中私密帖数量 */
        long privatePostCount,
        /** 我发表的评论数 */
        long commentCount,
        /** 我点赞过的帖子数 */
        long likedCount,
        /** 我收藏的帖子数 */
        long favoriteCount,
        long followerCount,
        long followeeCount,
        /** 我的帖子累计获得的赞 */
        long likeReceivedCount
) {
}
