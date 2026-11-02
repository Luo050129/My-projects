package com.luoxianjun.community.module.user.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.user.dto.response.UserStatsVO;
import com.luoxianjun.community.module.user.dto.response.UserSummaryVO;

/**
 * 用户社交关系：关注 / 粉丝，以及个人中心的聚合统计。
 */
public interface UserSocialService {

    /** 关注某人；重复关注幂等 */
    void follow(Long userId);

    /** 取消关注；未关注时幂等 */
    void unfollow(Long userId);

    /** 某人的粉丝列表 */
    PageResult<UserSummaryVO> pageFollowers(Long userId, long current, long size);

    /** 某人关注的人 */
    PageResult<UserSummaryVO> pageFollowees(Long userId, long current, long size);

    /** 用户摘要（含粉丝/关注/帖子数与「我是否已关注」） */
    UserSummaryVO summary(Long userId);

    /** 个人中心统计，固定取当前登录用户 */
    UserStatsVO myStats();

    boolean isFollowing(Long followerId, Long followeeId);

    long countFollowers(Long userId);

    long countFollowees(Long userId);
}
