package com.luoxianjun.community.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.entity.Comment;
import com.luoxianjun.community.module.content.entity.PostFavorite;
import com.luoxianjun.community.module.content.entity.PostLike;
import com.luoxianjun.community.module.content.mapper.CommentMapper;
import com.luoxianjun.community.module.content.mapper.PostFavoriteMapper;
import com.luoxianjun.community.module.content.mapper.PostLikeMapper;
import com.luoxianjun.community.module.content.service.PostService;
import com.luoxianjun.community.module.user.dto.response.UserStatsVO;
import com.luoxianjun.community.module.user.dto.response.UserSummaryVO;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.entity.UserFollow;
import com.luoxianjun.community.module.user.mapper.UserFollowMapper;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.module.user.service.UserSocialService;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSocialServiceImpl implements UserSocialService {

    private final UserFollowMapper userFollowMapper;
    private final UserMapper userMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final CommentMapper commentMapper;
    private final PostService postService;

    @Override
    @Transactional
    public void follow(Long userId) {
        Long me = SecurityUtil.currentUserId();
        if (me.equals(userId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能关注自己");
        }
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        UserFollow follow = new UserFollow();
        follow.setFollowerId(me);
        follow.setFolloweeId(userId);
        try {
            userFollowMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 唯一索引兜底：并发重复点击不会产生第二条记录，也不需要报错给用户
            log.debug("重复关注，已忽略：{} -> {}", me, userId);
        }
    }

    @Override
    @Transactional
    public void unfollow(Long userId) {
        Long me = SecurityUtil.currentUserId();
        userFollowMapper.delete(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, me)
                .eq(UserFollow::getFolloweeId, userId));
    }

    @Override
    public PageResult<UserSummaryVO> pageFollowers(Long userId, long current, long size) {
        Long viewer = SecurityUtil.currentUserIdOrNull();
        IPage<User> page = userFollowMapper.pageFollowers(new Page<>(current, size), userId);
        return PageResult.of(
                page.getRecords().stream().map(u -> toSummary(u, viewer)).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<UserSummaryVO> pageFollowees(Long userId, long current, long size) {
        Long viewer = SecurityUtil.currentUserIdOrNull();
        IPage<User> page = userFollowMapper.pageFollowees(new Page<>(current, size), userId);
        return PageResult.of(
                page.getRecords().stream().map(u -> toSummary(u, viewer)).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public UserSummaryVO summary(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return toSummary(user, SecurityUtil.currentUserIdOrNull());
    }

    @Override
    public UserStatsVO myStats() {
        Long me = SecurityUtil.currentUserId();
        long posts = postService.countPostsByUser(me, true);
        long publicPosts = postService.countPostsByUser(me, false);

        Long comments = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, me)
                .eq(Comment::getDeleted, 0));
        Long liked = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getUserId, me));
        Long favorited = postFavoriteMapper.selectCount(new LambdaQueryWrapper<PostFavorite>()
                .eq(PostFavorite::getUserId, me));

        return new UserStatsVO(
                posts,
                posts - publicPosts,
                comments == null ? 0 : comments,
                liked == null ? 0 : liked,
                favorited == null ? 0 : favorited,
                countFollowers(me),
                countFollowees(me),
                postService.sumLikesReceived(me));
    }

    @Override
    public boolean isFollowing(Long followerId, Long followeeId) {
        return userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, followerId)
                .eq(UserFollow::getFolloweeId, followeeId)) > 0;
    }

    @Override
    public long countFollowers(Long userId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFolloweeId, userId));
        return count == null ? 0 : count;
    }

    @Override
    public long countFollowees(Long userId) {
        Long count = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFollowerId, userId));
        return count == null ? 0 : count;
    }

    /**
     * 每个条目要查帖子数 / 粉丝数 / 关注数 / 关注关系，是 N+1。
     * 一页 10 条就是几十次轻量 count，比为此引入缓存或手写一条大 join 更划算；
     * 真实量级上来以后再换成聚合查询。
     */
    private UserSummaryVO toSummary(User u, Long viewerId) {
        boolean following = viewerId != null && !viewerId.equals(u.getId())
                && isFollowing(viewerId, u.getId());
        return new UserSummaryVO(
                u.getId(),
                u.getNickname(),
                u.getRole(),
                postService.countPostsByUser(u.getId(), false),
                countFollowers(u.getId()),
                countFollowees(u.getId()),
                following);
    }
}
