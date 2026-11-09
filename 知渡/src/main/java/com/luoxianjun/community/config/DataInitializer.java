package com.luoxianjun.community.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.luoxianjun.community.module.content.entity.Comment;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.entity.PostFavorite;
import com.luoxianjun.community.module.content.entity.PostLike;
import com.luoxianjun.community.module.content.mapper.CommentMapper;
import com.luoxianjun.community.module.content.mapper.PostFavoriteMapper;
import com.luoxianjun.community.module.content.mapper.PostLikeMapper;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.entity.UserFollow;
import com.luoxianjun.community.module.user.mapper.UserFollowMapper;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 启动种子数据。
 * <p>
 * 分段判断而不是"用户表有数据就整体跳过"：这样老库升级后也能补上新增的用户与帖子，
 * 同时不会重复插入已经存在的行。每段都独立幂等。
 */
@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final UserFollowMapper userFollowMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Long> userIds = ensureUsers();
        List<Long> postIds = ensurePosts(userIds);
        ensureComments(userIds, postIds);
        ensureLikes(userIds, postIds);
        ensureFavorites(userIds, postIds);
        ensureFollows(userIds);
    }

    /** 按 username 补插缺失用户，已存在的直接复用 id */
    private Map<String, Long> ensureUsers() {
        Map<String, Long> ids = new HashMap<>();
        int inserted = 0;

        for (SeedData.SeedUser seed : SeedData.USERS) {
            User existing = userMapper.selectOne(
                    new LambdaQueryWrapper<User>().eq(User::getUsername, seed.username()));
            if (existing != null) {
                ids.put(seed.username(), existing.getId());
                continue;
            }
            User user = new User();
            user.setUsername(seed.username());
            user.setPassword(passwordEncoder.encode(seed.password()));
            user.setNickname(seed.nickname());
            user.setRole(seed.role());
            user.setStatus(1);
            user.setCreateTime(LocalDateTime.now().minusDays(seed.daysAgo()));
            userMapper.insert(user);
            ids.put(seed.username(), user.getId());
            inserted++;
        }

        if (inserted > 0) {
            log.info("已补充种子用户 {} 个：admin/admin123、alice/alice123、bob/bob123、carol/carol123、david/david123、eve/eve123",
                    inserted);
        }
        return ids;
    }

    /**
     * 逐个种子帖判重（作者 + 标题）后补插，返回与 SeedData.POSTS 顺序严格对齐的 id 列表。
     * <p>
     * 之所以不用「帖子表非空就整体跳过」：老库升级时新增的种子帖会被整体跳过，
     * 而按下标引用的评论 / 点赞也会错位到别的帖子上。
     * 作者缺失时塞 null 占位，保持下标对齐，下游 pick 拿到 null 自动跳过。
     */
    private List<Long> ensurePosts(Map<String, Long> userIds) {
        List<Long> ids = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;

        for (SeedData.SeedPost seed : SeedData.POSTS) {
            Long authorId = userIds.get(seed.author());
            if (authorId == null) {
                log.warn("种子帖作者 {} 不存在，跳过：{}", seed.author(), seed.title());
                ids.add(null);
                continue;
            }

            Post existing = postMapper.selectOne(new LambdaQueryWrapper<Post>()
                    .eq(Post::getUserId, authorId)
                    .eq(Post::getTitle, seed.title()));
            if (existing != null) {
                ids.add(existing.getId());
                continue;
            }

            Post post = new Post();
            post.setUserId(authorId);
            post.setTitle(seed.title());
            post.setContent(seed.content());
            post.setSummary(seed.summary());
            post.setTags(seed.tags());
            post.setCoverImage(seed.cover());
            post.setPostType(seed.postType());
            post.setVisibility(seed.visibility());
            post.setStatus(1);
            post.setViewCount(0);
            post.setLikeCount(0);
            post.setCommentCount(0);
            // 只有图文帖标成「已 AI 增强」：全标上会让列表里每条都挂 AI 摘要标签，看着太假
            post.setAiEnhanced(Post.TYPE_IMAGE.equals(seed.postType()) && seed.summary() != null ? 1 : 0);
            post.setCreateTime(now.minusHours(seed.hoursAgo()));
            post.setUpdateTime(post.getCreateTime());
            postMapper.insert(post);
            ids.add(post.getId());
            inserted++;
        }

        if (inserted > 0) {
            log.info("已补充种子帖子 {} 条（种子共 {} 条）", inserted, ids.size());
        }
        return ids;
    }

    private void ensureComments(Map<String, Long> userIds, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;
        for (SeedData.SeedComment seed : SeedData.COMMENTS) {
            Long postId = pick(postIds, seed.postIndex());
            Long userId = userIds.get(seed.author());
            if (postId == null || userId == null) {
                continue;
            }
            // 逐条判重（帖子 + 用户 + 内容）：既能补插，又不会在重启后重复堆叠
            Long exists = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getPostId, postId)
                    .eq(Comment::getUserId, userId)
                    .eq(Comment::getContent, seed.content()));
            if (exists != null && exists > 0) {
                continue;
            }
            Comment comment = new Comment();
            comment.setPostId(postId);
            comment.setUserId(userId);
            comment.setParentId(0L);
            comment.setContent(seed.content());
            comment.setStatus(1);
            comment.setCreateTime(now.minusHours(seed.hoursAgo()));
            commentMapper.insert(comment);
            inserted++;
        }

        // 评论数按实际插入结果回填，避免硬编码数字和实际行数对不上
        for (Long postId : postIds) {
            Long count = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getPostId, postId)
                    .eq(Comment::getDeleted, 0));
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .set(Post::getCommentCount, count == null ? 0 : count.intValue()));
        }
        log.info("已初始化种子评论 {} 条", inserted);
    }

    private void ensureLikes(Map<String, Long> userIds, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return;
        }

        int inserted = 0;
        for (SeedData.SeedLikes seed : SeedData.LIKES) {
            Long postId = pick(postIds, seed.postIndex());
            if (postId == null) {
                continue;
            }
            for (String username : seed.users()) {
                Long userId = userIds.get(username);
                if (userId == null) {
                    continue;
                }
                Long exists = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, postId)
                        .eq(PostLike::getUserId, userId));
                if (exists != null && exists > 0) {
                    continue;
                }
                PostLike like = new PostLike();
                like.setPostId(postId);
                like.setUserId(userId);
                postLikeMapper.insert(like);
                inserted++;
            }
        }

        for (Long postId : postIds) {
            Long count = postLikeMapper.selectCount(
                    new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, postId));
            postMapper.update(null, new LambdaUpdateWrapper<Post>()
                    .eq(Post::getId, postId)
                    .set(Post::getLikeCount, count == null ? 0 : count.intValue()));
        }
        log.info("已初始化种子点赞 {} 条", inserted);
    }

    private void ensureFavorites(Map<String, Long> userIds, List<Long> postIds) {
        if (postIds.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        int inserted = 0;
        for (SeedData.SeedFavorite seed : SeedData.FAVORITES) {
            Long postId = pick(postIds, seed.postIndex());
            Long userId = userIds.get(seed.user());
            if (postId == null || userId == null) {
                continue;
            }
            Long exists = postFavoriteMapper.selectCount(new LambdaQueryWrapper<PostFavorite>()
                    .eq(PostFavorite::getPostId, postId)
                    .eq(PostFavorite::getUserId, userId));
            if (exists != null && exists > 0) {
                continue;
            }
            PostFavorite favorite = new PostFavorite();
            favorite.setPostId(postId);
            favorite.setUserId(userId);
            favorite.setCreateTime(now.minusHours(seed.hoursAgo()));
            postFavoriteMapper.insert(favorite);
            inserted++;
        }
        log.info("已初始化种子收藏 {} 条", inserted);
    }

    private void ensureFollows(Map<String, Long> userIds) {
        int inserted = 0;
        for (SeedData.SeedFollow seed : SeedData.FOLLOWS) {
            Long follower = userIds.get(seed.follower());
            Long followee = userIds.get(seed.followee());
            if (follower == null || followee == null || follower.equals(followee)) {
                continue;
            }
            Long exists = userFollowMapper.selectCount(new LambdaQueryWrapper<UserFollow>()
                    .eq(UserFollow::getFollowerId, follower)
                    .eq(UserFollow::getFolloweeId, followee));
            if (exists != null && exists > 0) {
                continue;
            }
            UserFollow follow = new UserFollow();
            follow.setFollowerId(follower);
            follow.setFolloweeId(followee);
            userFollowMapper.insert(follow);
            inserted++;
        }
        log.info("已初始化种子关注关系 {} 条", inserted);
    }

    /** 下标越界一律返回 null，让上游跳过而不是整个初始化失败 */
    private Long pick(List<Long> ids, int index) {
        if (index < 0 || index >= ids.size()) {
            return null;
        }
        return ids.get(index);
    }
}
