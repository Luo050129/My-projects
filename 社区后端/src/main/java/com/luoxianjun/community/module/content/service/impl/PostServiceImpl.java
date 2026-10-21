package com.luoxianjun.community.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.util.RedisLock;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.dto.request.UpdatePostRequest;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.PostLikeMapper;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.PostService;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final UserMapper userMapper;
    private final PostCacheService postCache;
    private final RedisLock redisLock;

    /** 缓存未命中时自旋等待缓存被回填的参数：最多等 2s（100 × 20ms） */
    private static final int CACHE_SPIN_TIMES = 100;
    private static final long CACHE_SPIN_SLEEP_MS = 20L;

    @Override
    @Transactional
    public Long createPost(CreatePostRequest request) {
        Post post = new Post();
        post.setUserId(SecurityUtil.currentUserId());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setTags(request.tags());
        post.setStatus(1);
        post.setViewCount(0);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setAiEnhanced(0);
        postMapper.insert(post);
        return post.getId();
    }

    @Override
    @Transactional
    public void updatePost(Long id, UpdatePostRequest request) {
        Post post = requireOwnedPost(id);
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setTags(request.tags());
        postMapper.updateById(post);
        postCache.evict(id);
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        Post post = requireOwnedPost(id);
        post.setDeleted(1);
        postMapper.updateById(post);
        postCache.evict(id);
    }

    @Override
    public PostVO getDetail(Long id) {
        Optional<PostVO> cached = postCache.get(id);
        if (cached.isPresent()) {
            return unwrap(cached.get());
        }
        // 缓存未命中：抢到锁的线程回源写缓存，其余线程自旋等待缓存就绪（防击穿）
        return unwrap(redisLock.lockWithWait(
                RedisLock.lockKeyOf("post", id), 3000,
                () -> postCache.get(id),
                () -> loadFromDb(id),
                CACHE_SPIN_TIMES, CACHE_SPIN_SLEEP_MS));
    }

    private PostVO unwrap(PostVO vo) {
        if (postCache.isNullMarker(vo)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        return vo;
    }

    /** 回源查库：浏览量仅在回源时累加（缓存命中不计），保证 DB 为权威值 */
    private PostVO loadFromDb(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())
                || !Integer.valueOf(1).equals(post.getStatus())) {
            postCache.putNull(id);
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        post.setViewCount(post.getViewCount() + 1);
        postMapper.updateById(post);
        PostVO vo = toVO(post);
        postCache.put(vo);
        return vo;
    }

    @Override
    public PageResult<PostVO> pagePosts(String keyword, long current, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0);
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Post::getTitle, keyword);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        IPage<Post> page = postMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public void toggleLike(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getDeleted() == 1 || post.getStatus() != 1) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        Long uid = SecurityUtil.currentUserId();
        Long existed = postLikeMapper.selectCount(
                new LambdaQueryWrapper<com.luoxianjun.community.module.content.entity.PostLike>()
                        .eq(com.luoxianjun.community.module.content.entity.PostLike::getPostId, id)
                        .eq(com.luoxianjun.community.module.content.entity.PostLike::getUserId, uid));
        int delta;
        if (existed != null && existed > 0) {
            // 取消点赞
            postLikeMapper.delete(
                    new LambdaQueryWrapper<com.luoxianjun.community.module.content.entity.PostLike>()
                            .eq(com.luoxianjun.community.module.content.entity.PostLike::getPostId, id)
                            .eq(com.luoxianjun.community.module.content.entity.PostLike::getUserId, uid));
            delta = -1;
        } else {
            // 点赞（唯一索引兜底幂等）
            com.luoxianjun.community.module.content.entity.PostLike like =
                    new com.luoxianjun.community.module.content.entity.PostLike();
            like.setPostId(id);
            like.setUserId(uid);
            postLikeMapper.insert(like);
            delta = 1;
        }
        // 原子自增：避免「读-改-写」在并发下丢失更新（多个点赞互相覆盖计数）
        String op = delta > 0 ? "+ 1" : "- 1";
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .setSql("like_count = CASE WHEN like_count " + op + " < 0 THEN 0 ELSE like_count " + op + " END"));
        // 同步更新缓存中的点赞数（避免下次回源重复累加浏览量）
        postCache.get(id).ifPresent(vo -> {
            if (!postCache.isNullMarker(vo)) {
                PostVO updated = new PostVO(vo.id(), vo.authorId(), vo.authorName(), vo.title(),
                        vo.summary(), vo.tags(), vo.status(), vo.viewCount(),
                        vo.likeCount() + delta, vo.commentCount(), vo.aiEnhanced(), vo.createTime());
                postCache.put(updated);
            }
        });
    }

    @Override
    @Transactional
    public void applyAiEnhancement(Long postId, String summary, String tags) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        post.setSummary(summary);
        post.setTags(tags);
        post.setAiEnhanced(1);
        postMapper.updateById(post);
        postCache.evict(postId);
    }

    private Post requireOwnedPost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getDeleted() == 1) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        Long uid = SecurityUtil.currentUserId();
        if (!post.getUserId().equals(uid) && !SecurityUtil.isAdmin()) {
            throw new BizException(ErrorCode.POST_NOT_OWNER);
        }
        return post;
    }

    private PostVO toVO(Post p) {
        User author = userMapper.selectById(p.getUserId());
        String name = author != null ? author.getNickname() : "未知用户";
        return new PostVO(p.getId(), p.getUserId(), name, p.getTitle(), p.getSummary(),
                p.getTags(), p.getStatus(), p.getViewCount(), p.getLikeCount(),
                p.getCommentCount(), p.getAiEnhanced(), p.getCreateTime());
    }
}
