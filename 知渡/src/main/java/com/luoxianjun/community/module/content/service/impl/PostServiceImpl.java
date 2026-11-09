package com.luoxianjun.community.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.util.RedisLock;
import com.luoxianjun.community.common.util.TransactionSupport;
import com.luoxianjun.community.config.StorageProperties;
import com.luoxianjun.community.module.notify.publisher.NotificationPublisher;
import com.luoxianjun.community.module.governance.GovernanceService;
import com.luoxianjun.community.module.governance.entity.ContentReview;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.dto.PostInteractionVO;
import com.luoxianjun.community.module.content.dto.PostSuggestVO;
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
import com.luoxianjun.community.module.content.search.PostKeyword;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.security.SecurityUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
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
    private final NotificationPublisher notificationPublisher;
    private final GovernanceService governanceService;

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

    /** 当前访问者能否读这篇帖子：私密、待审、驳回都只对作者与管理员开放 */
    private boolean isReadableForViewer(Post post) {
        if (!Post.isPublicReview(post.getReviewStatus())) {
            return isOwnerOrAdmin(post.getUserId());
        }
        return !isPrivate(post) || isOwnerOrAdmin(post.getUserId());
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
        // 显式 / 隐式 AI 标识：作者声明 + 结构化声明元数据
        boolean declared = Boolean.TRUE.equals(request.aiGenerated());
        post.setAiGenerated(declared ? 1 : 0);
        post.setAiDeclaration(governanceService.buildAiDeclaration(post.getUserId(), declared));
        // 发帖前置规则检查：命中不删内容，只转人工审核
        String hit = governanceService.checkRules(request.title(), request.content());
        post.setReviewStatus(hit == null ? Post.REVIEW_EXEMPT : Post.REVIEW_PENDING);
        postMapper.insert(post);
        if (hit != null) {
            governanceService.openReview(post.getId(), post.getUserId(),
                    ContentReview.SOURCE_RULE, "命中规则词：" + hit);
        }
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
        if (request.aiGenerated() != null) {
            boolean declared = Boolean.TRUE.equals(request.aiGenerated());
            post.setAiGenerated(declared ? 1 : 0);
            post.setAiDeclaration(governanceService.buildAiDeclaration(post.getUserId(), declared));
        }
        // 编辑同样要过规则：不能靠「发帖时查一次」绕过治理
        String hit = governanceService.checkRules(request.title(), request.content());
        if (hit != null) {
            post.setReviewStatus(Post.REVIEW_PENDING);
        }
        postMapper.updateById(post);
        postCache.evict(id);
        if (hit != null) {
            governanceService.openReview(id, post.getUserId(),
                    ContentReview.SOURCE_RULE, "编辑后命中规则词：" + hit);
        }
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

        // 私密 / 待审 / 驳回内容只对作者与管理员可见。
        // 这里再判一次而不是只靠「不写缓存」：状态切换的瞬间缓存可能还是旧值。
        if (!isOwnerOrAdmin(vo.authorId())
                && (Integer.valueOf(Post.VISIBILITY_PRIVATE).equals(vo.visibility())
                    || !Post.isPublicReview(vo.reviewStatus()))) {
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
        // 「存在但对当前访问者不可读」不能写空标记：
        // 否则作者稍后查看自己的待审帖会命中空标记，被误判为不存在。
        if (!isReadableForViewer(post)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        // 浏览量同样下推 DB 原子自增：自旋超时分支不持锁，读-改-写会丢更新
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .setSql("view_count = CASE WHEN view_count + 1 < 0 THEN 0 ELSE view_count + 1 END"));
        // 只同步内存中的展示值，DB 仍是权威；不再用 updateById 覆盖整行
        post.setViewCount((post.getViewCount() == null ? 0 : post.getViewCount()) + 1);
        PostVO vo = toVO(post, post.getContent());
        if (isPrivate(post) || !Post.isPublicReview(post.getReviewStatus())) {
            // 缓存是所有用户共享的，私密 / 待审 / 驳回帖写进去等于泄露给下一个访问者。
            // 代价是这类帖子详情每次都回源，但这本来就只有作者一个人在看。
            log.debug("非公开帖 {} 跳过写缓存", id);
        } else {
            postCache.put(vo);
        }
        return vo;
    }

    /** 公开可读帖子的统一过滤条件：列表与联想必须共用，避免联想泄露私密 / 待审内容 */
    private LambdaQueryWrapper<Post> publicWrapper() {
        return new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .eq(Post::getDeleted, 0)
                // 公开列表只出公开帖：作者自己的私密帖在个人中心看，混进列表会让「私密」名不副实
                .eq(Post::getVisibility, Post.VISIBILITY_PUBLIC)
                // 待审 / 驳回内容不进公开列表，否则审核等于形同虚设
                .in(Post::getReviewStatus, Post.PUBLIC_READABLE_REVIEW);
    }

    /**
     * 多词包含匹配：每个词在标题 / 摘要 / 正文 / 标签里命中即可，词与词之间也是 OR。
     * 全部条件下推到 SQL，参数由 MyBatis-Plus 绑定，不在应用层捞出后再过滤。
     */
    private void applyKeyword(LambdaQueryWrapper<Post> wrapper, List<String> words) {
        if (words.isEmpty()) {
            return;
        }
        wrapper.and(outer -> {
            for (int i = 0; i < words.size(); i++) {
                String pattern = "%" + words.get(i) + "%";
                // 首个词用 and、后续词用 or(Consumer)：
                // 若统一用 and(Consumer)，MyBatis-Plus 会强制以 AND 连接，
                // 多词查询会退化成「必须同时包含所有词」，与「任一词命中」的语义相反。
                if (i == 0) {
                    outer.and(inner -> applyWordCondition(inner, pattern));
                } else {
                    outer.or(inner -> applyWordCondition(inner, pattern));
                }
            }
        });
    }

    /** 一个词在四个字段上的包含匹配，显式 LOWER 抹平 H2 / MySQL 的大小写差异 */
    private void applyWordCondition(LambdaQueryWrapper<Post> inner, String pattern) {
        // 列名是常量，用户输入只经 {0} 参数绑定，不存在拼接注入
        inner.apply("LOWER(title) LIKE LOWER({0})", pattern)
                .or().apply("LOWER(summary) LIKE LOWER({0})", pattern)
                .or().apply("LOWER(content) LIKE LOWER({0})", pattern)
                .or().apply("LOWER(tags) LIKE LOWER({0})", pattern);
    }

    @Override
    public PageResult<PostVO> pagePosts(String keyword, long current, long size) {
        List<String> words = PostKeyword.split(keyword);
        LambdaQueryWrapper<Post> wrapper = publicWrapper();
        applyKeyword(wrapper, words);
        if (words.isEmpty()) {
            wrapper.orderByDesc(Post::getCreateTime);
        } else {
            // 搜索结果按点赞数排序；id 作次级键，保证同赞数时翻页稳定
            wrapper.orderByDesc(Post::getLikeCount).orderByDesc(Post::getId);
        }
        IPage<Post> page = postMapper.selectPage(new Page<>(current, size), wrapper);
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<PostSuggestVO> suggest(String keyword, int limit) {
        List<String> words = PostKeyword.split(keyword);
        if (words.isEmpty()) {
            return List.of();
        }
        int n = Math.min(Math.max(limit, 1), 20);
        LambdaQueryWrapper<Post> wrapper = publicWrapper();
        applyKeyword(wrapper, words);
        wrapper.orderByDesc(Post::getLikeCount).orderByDesc(Post::getId);
        IPage<Post> page = postMapper.selectPage(new Page<>(1, n), wrapper);
        return page.getRecords().stream().map(p -> toSuggest(p, words)).toList();
    }

    private PostSuggestVO toSuggest(Post p, List<String> words) {
        User author = userMapper.selectById(p.getUserId());
        String name = author != null ? author.getNickname() : "未知用户";
        String matched = words.stream()
                .filter(w -> containsIgnoreCase(p.getTitle(), w)
                        || containsIgnoreCase(p.getSummary(), w)
                        || containsIgnoreCase(p.getContent(), w)
                        || containsIgnoreCase(p.getTags(), w))
                .findFirst().orElse(words.get(0));
        return new PostSuggestVO(p.getId(), p.getTitle(), snippet(p, matched), name,
                p.getLikeCount(), p.getCommentCount(), matched);
    }

    private boolean containsIgnoreCase(String text, String word) {
        return text != null && word != null
                && text.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
    }

    /** 从摘要 / 正文 / 标签截一段命中词附近的上下文；标题已在 title 中展示，不再重复 */
    private String snippet(Post p, String word) {
        String[] fields = {p.getSummary(), p.getContent(), p.getTags()};
        for (String field : fields) {
            if (!containsIgnoreCase(field, word)) {
                continue;
            }
            String text = field.replaceAll("\\s+", " ");
            int idx = text.toLowerCase(Locale.ROOT).indexOf(word.toLowerCase(Locale.ROOT));
            int start = Math.max(0, idx - 18);
            int end = Math.min(text.length(), idx + word.length() + 32);
            return (start > 0 ? "…" : "") + text.substring(start, end)
                    + (end < text.length() ? "…" : "");
        }
        return "";
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
            wrapper.eq(Post::getVisibility, Post.VISIBILITY_PUBLIC)
                    .in(Post::getReviewStatus, Post.PUBLIC_READABLE_REVIEW);
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
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())
                || !Integer.valueOf(1).equals(post.getStatus())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        // 私密帖对非作者等同于不存在
        if (!isReadableForViewer(post)) {
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
            PostLike like = new PostLike();
            like.setPostId(id);
            like.setUserId(uid);
            try {
                postLikeMapper.insert(like);
            } catch (DuplicateKeyException e) {
                // 同一用户并发点击：唯一索引兜住了脏数据，这里转成幂等成功，不再向调用方抛 500
                log.debug("并发点赞命中唯一索引，按幂等处理 postId={} userId={}", id, uid);
                return;
            }
            delta = 1;
        }
        // 原子自增：避免「读-改-写」在并发下丢失更新（多个点赞互相覆盖计数）
        String op = delta > 0 ? "+ 1" : "- 1";
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .setSql("like_count = CASE WHEN like_count " + op + " < 0 THEN 0 ELSE like_count " + op + " END"));
        // 缓存计数以 DB 最新值为准重建，不用 vo.likeCount()+delta（并发下会读到旧值）
        postCache.get(id).ifPresent(vo -> {
            if (!postCache.isNullMarker(vo)) {
                Post fresh = postMapper.selectById(id);
                int likeCount = fresh != null && fresh.getLikeCount() != null
                        ? fresh.getLikeCount()
                        : (vo.likeCount() == null ? 0 : vo.likeCount());
                PostVO updated = new PostVO(vo.id(), vo.authorId(), vo.authorName(), vo.title(),
                        vo.summary(), vo.content(), vo.tags(), vo.coverImage(), vo.postType(),
                        vo.visibility(), vo.status(), vo.viewCount(),
                        likeCount, vo.commentCount(), vo.aiEnhanced(),
                        vo.aiGenerated(), vo.reviewStatus(), vo.createTime());
                postCache.put(updated);
            }
        });

        // 点赞也异步通知帖主；提交后再投递，事务回滚时不会留下幽灵通知
        if (delta > 0 && !post.getUserId().equals(uid)) {
            User liker = userMapper.selectById(uid);
            String name = liker != null ? liker.getNickname() : "有人";
            String content = name + " 赞了你的帖子《" + post.getTitle() + "》";
            Long receiverId = post.getUserId();
            TransactionSupport.afterCommit(() ->
                    notificationPublisher.publish(receiverId, "LIKE", content, id));
        }
    }

    @Override
    @Transactional
    public boolean toggleFavorite(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())
                || !Integer.valueOf(1).equals(post.getStatus())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        if (!isReadableForViewer(post)) {
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
        try {
            postFavoriteMapper.insert(favorite);
        } catch (DuplicateKeyException e) {
            // 并发重复收藏：唯一索引兜底，按「已收藏」幂等返回
            log.debug("并发收藏命中唯一索引，按幂等处理 postId={} userId={}", id, uid);
        }
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
                p.getCommentCount(), p.getAiEnhanced(),
                Integer.valueOf(1).equals(p.getAiGenerated()), p.getReviewStatus(),
                p.getCreateTime());
    }
}
