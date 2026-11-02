package com.luoxianjun.community.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.util.RedisLock;
import com.luoxianjun.community.config.StorageProperties;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.dto.PostInteractionVO;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.dto.request.UpdatePostRequest;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.entity.PostFavorite;
import com.luoxianjun.community.module.content.entity.PostLike;
import com.luoxianjun.community.module.content.mapper.PostFavoriteMapper;
import com.luoxianjun.community.module.content.mapper.PostLikeMapper;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.PostService;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.security.SecurityUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final PostFavoriteMapper postFavoriteMapper;
    private final UserMapper userMapper;
    private final PostCacheService postCache;
    private final RedisLock redisLock;
    private final StorageProperties storageProperties;

    /**
     * 封面图白名单：只接受本服务上传接口产出的路径。
     * 校验放在服务端而不是只靠前端，因为请求可以被绕过；
     * 允许任意 URL 会带来两个问题：外链失效后页面破图，删帖时也无法回收本地文件。
     */
    private Pattern coverPattern;

    @PostConstruct
    void initCoverPattern() {
        String prefix = Pattern.quote(storageProperties.getPublicPrefix());
        this.coverPattern = Pattern.compile(prefix + "/\\d{4}/\\d{2}/[0-9a-f]{32}\\.(png|jpg|gif|webp)");
    }

    /** 空值归一为 null；非空则必须匹配上传路径，否则拒绝而不是静默丢弃 */
    private String normalizeCoverImage(String coverImage) {
        if (coverImage == null || coverImage.isBlank()) {
            return null;
        }
        String v = coverImage.trim();
        if (!coverPattern.matcher(v).matches()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "封面图地址不合法，请先通过上传接口获取");
        }
        return v;
    }

    /**
     * 帖子形态归一：
     * - 显式 IMAGE 但没封面 → 拒绝（前端选了图文却没上传图，属于无效请求）
     * - 带了封面但说自己是 TEXT → 按 IMAGE 处理（宽容，不因为字段没对齐就报错）
     * - 其他一律 TEXT
     */
    private String normalizePostType(String postType, String coverImage) {
        String type = postType == null ? "" : postType.trim().toUpperCase();
        if (Post.TYPE_IMAGE.equals(type)) {
            if (coverImage == null) {
                throw new BizException(ErrorCode.PARAM_INVALID, "图文帖需要先上传封面图");
            }
            return Post.TYPE_IMAGE;
        }
        if (Post.TYPE_TEXT.equals(type)) {
            return coverImage == null ? Post.TYPE_TEXT : Post.TYPE_IMAGE;
        }
        return coverImage == null ? Post.TYPE_TEXT : Post.TYPE_IMAGE;
    }

    private int normalizeVisibility(Integer visibility) {
        return visibility != null && visibility == Post.VISIBILITY_PRIVATE
                ? Post.VISIBILITY_PRIVATE
                : Post.VISIBILITY_PUBLIC;
    }

    private boolean isPrivate(Post post) {
        return post.getVisibility() != null && post.getVisibility() == Post.VISIBILITY_PRIVATE;
    }

    private boolean isOwnerOrAdmin(Long authorId) {
        Long uid = SecurityUtil.currentUserIdOrNull();
        return uid != null && (uid.equals(authorId) || SecurityUtil.isAdmin());
    }

    /** 缓存未命中时自旋等待缓存被回填的参数：最多等 2s（100 × 20ms） */
    private static final int CACHE_SPIN_TIMES = 100;
    private static final long CACHE_SPIN_SLEEP_MS = 20L;

    @Override
    @Transactional
    public Long createPost(CreatePostRequest request) {
        String cover = normalizeCoverImage(request.coverImage());
        Post post = new Post();
        post.setUserId(SecurityUtil.currentUserId());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setTags(request.tags());
        post.setCoverImage(cover);
        post.setPostType(normalizePostType(request.postType(), cover));
        post.setVisibility(normalizeVisibility(request.visibility()));
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
        String cover = normalizeCoverImage(request.coverImage());
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setTags(request.tags());
        post.setCoverImage(cover);
        post.setPostType(normalizePostType(request.postType(), cover));
        post.setVisibility(normalizeVisibility(request.visibility()));
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
        PostVO vo = cached.isPresent()
                ? unwrap(cached.get())
                // 缓存未命中：抢到锁的线程回源写缓存，其余线程自旋等待缓存就绪（防击穿）
                : unwrap(redisLock.lockWithWait(
                        RedisLock.lockKeyOf("post", id), 3000,
                        () -> postCache.get(id),
                        () -> loadFromDb(id),
                        CACHE_SPIN_TIMES, CACHE_SPIN_SLEEP_MS));

        // 私密帖只对作者与管理员可见。这里再判一次而不是只靠「不写缓存」：
        // 帖子从公开改成私密的瞬间，缓存可能还没失效完。
        if (vo.visibility() != null && vo.visibility() == Post.VISIBILITY_PRIVATE
                && !isOwnerOrAdmin(vo.authorId())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        return vo;
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
        PostVO vo = toVO(post, post.getContent());
        if (isPrivate(post)) {
            // 缓存是所有用户共享的，私密帖写进去等于泄露给下一个访问者。
            // 代价是私密帖详情每次都回源，但这本来就只有作者一个人在看。
            log.debug("私密帖 {} 跳过写缓存", id);
        } else {
            postCache.put(vo);
        }
        return vo;
    }

    @Override
    public PageResult<PostVO> pagePosts(String keyword, long current, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0)
                // 公开列表只出公开帖：作者自己的私密帖在个人中心看，混进列表会让「私密」名不副实
                .eq(Post::getVisibility, Post.VISIBILITY_PUBLIC);
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
    public PageResult<PostVO> pageUserPosts(Long userId, boolean includePrivate, long current, long size) {
        // 防御：includePrivate 只是「希望包含」，能不能包含由当前身份决定，
        // 否则任何人都能拼一个参数把别人的私密帖拉走。
        boolean permitPrivate = includePrivate && isOwnerOrAdmin(userId);

        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0)
                .eq(Post::getUserId, userId);
        if (!permitPrivate) {
            wrapper.eq(Post::getVisibility, Post.VISIBILITY_PUBLIC);
        }
        wrapper.orderByDesc(Post::getCreateTime);
        IPage<Post> page = postMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<PostVO> pageLikedPosts(Long userId, long current, long size) {
        IPage<Post> page = postLikeMapper.pageLikedByUser(new Page<>(current, size), userId);
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<PostVO> pageFavoritedPosts(Long userId, long current, long size) {
        IPage<Post> page = postFavoriteMapper.pageFavoritedByUser(new Page<>(current, size), userId);
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
        // 私密帖对非作者等同于不存在
        if (isPrivate(post) && !isOwnerOrAdmin(post.getUserId())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }

        Long uid = SecurityUtil.currentUserId();
        Long existed = postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>()
                        .eq(PostLike::getPostId, id)
                        .eq(PostLike::getUserId, uid));
        int delta;
        if (existed != null && existed > 0) {
            // 取消点赞
            postLikeMapper.delete(
                    new LambdaQueryWrapper<PostLike>()
                            .eq(PostLike::getPostId, id)
                            .eq(PostLike::getUserId, uid));
            delta = -1;
        } else {
            // 点赞（唯一索引兜底幂等）
            PostLike like = new PostLike();
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
                        vo.summary(), vo.content(), vo.tags(), vo.coverImage(), vo.postType(),
                        vo.visibility(), vo.status(), vo.viewCount(),
                        vo.likeCount() + delta, vo.commentCount(), vo.aiEnhanced(), vo.createTime());
                postCache.put(updated);
            }
        });
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || post.getDeleted() == 1 || post.getStatus() != 1) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        if (isPrivate(post) && !isOwnerOrAdmin(post.getUserId())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }

        Long uid = SecurityUtil.currentUserId();
        Long existed = postFavoriteMapper.selectCount(
                new LambdaQueryWrapper<PostFavorite>()
                        .eq(PostFavorite::getPostId, id)
                        .eq(PostFavorite::getUserId, uid));
        if (existed != null && existed > 0) {
            postFavoriteMapper.delete(
                    new LambdaQueryWrapper<PostFavorite>()
                            .eq(PostFavorite::getPostId, id)
                            .eq(PostFavorite::getUserId, uid));
            return false;
        }
        PostFavorite favorite = new PostFavorite();
        favorite.setPostId(id);
        favorite.setUserId(uid);
        postFavoriteMapper.insert(favorite);
        return true;
    }

    @Override
    public PostInteractionVO interaction(Long id) {
        Long uid = SecurityUtil.currentUserIdOrNull();
        if (uid == null) {
            return new PostInteractionVO(false, false);
        }
        boolean liked = postLikeMapper.selectCount(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, id).eq(PostLike::getUserId, uid)) > 0;
        boolean favorited = postFavoriteMapper.selectCount(new LambdaQueryWrapper<PostFavorite>()
                .eq(PostFavorite::getPostId, id).eq(PostFavorite::getUserId, uid)) > 0;
        return new PostInteractionVO(liked, favorited);
    }

    @Override
    public long countPostsByUser(Long userId, boolean includePrivate) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .eq(Post::getDeleted, 0);
        if (!includePrivate) {
            wrapper.eq(Post::getVisibility, Post.VISIBILITY_PUBLIC);
        }
        Long count = postMapper.selectCount(wrapper);
        return count == null ? 0 : count;
    }

    @Override
    public long sumLikesReceived(Long userId) {
        return postMapper.sumLikesByUser(userId);
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

    /** 列表场景：不返回正文，减小响应体 */
    private PostVO toVO(Post p) {
        return toVO(p, null);
    }

    /** 详情场景：带上正文 */
    private PostVO toVO(Post p, String content) {
        User author = userMapper.selectById(p.getUserId());
        String name = author != null ? author.getNickname() : "未知用户";
        return new PostVO(p.getId(), p.getUserId(), name, p.getTitle(), p.getSummary(),
                content, p.getTags(), p.getCoverImage(), p.getPostType(), p.getVisibility(),
                p.getStatus(), p.getViewCount(), p.getLikeCount(),
                p.getCommentCount(), p.getAiEnhanced(), p.getCreateTime());
    }
}
