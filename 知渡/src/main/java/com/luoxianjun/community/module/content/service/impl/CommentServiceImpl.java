package com.luoxianjun.community.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.util.TransactionSupport;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.dto.CommentVO;
import com.luoxianjun.community.module.content.dto.MyCommentVO;
import com.luoxianjun.community.module.content.dto.request.CommentRequest;
import com.luoxianjun.community.module.content.entity.Comment;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.CommentMapper;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.CommentService;
import com.luoxianjun.community.module.notify.publisher.NotificationPublisher;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final NotificationPublisher notificationPublisher;
    private final PostCacheService postCache;

    @Override
    @Transactional
    public Long addComment(CommentRequest request) {
        // 私密帖对非作者等同于不存在：评论与评论列表都必须走同一道校验
        Post post = requireVisiblePost(request.postId());
        Long uid = SecurityUtil.currentUserId();
        Comment comment = new Comment();
        comment.setPostId(request.postId());
        comment.setUserId(uid);
        comment.setParentId(request.parentId() == null ? 0L : request.parentId());
        comment.setContent(request.content());
        comment.setStatus(1);
        commentMapper.insert(comment);

        // 计数下推数据库：一条原子 SQL，避免「读-改-写」在并发下丢失更新
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, post.getId())
                .setSql("comment_count = CASE WHEN comment_count + 1 < 0 THEN 0 ELSE comment_count + 1 END"));
        // PostVO 带 comment_count，不失效缓存会让详情一直显示旧数字
        postCache.evict(post.getId());

        // 事务提交后再投递：业务回滚时不会留下幽灵通知
        if (!post.getUserId().equals(uid)) {
            User commenter = userMapper.selectById(uid);
            String name = commenter != null ? commenter.getNickname() : "有人";
            String content = name + " 评论了你的帖子《" + post.getTitle() + "》";
            Long receiverId = post.getUserId();
            Long postId = post.getId();
            TransactionSupport.afterCommit(() ->
                    notificationPublisher.publish(receiverId, "COMMENT", content, postId));
        }
        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> listComments(Long postId, long current, long size) {
        // 评论列表同样属于帖子内容，匿名请求也要挡住私密帖
        requireVisiblePost(postId);
        IPage<Comment> page = commentMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getPostId, postId)
                        .eq(Comment::getDeleted, 0)
                        .orderByAsc(Comment::getCreateTime));
        return PageResult.of(
                page.getRecords().stream().map(this::toVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<MyCommentVO> pageMyComments(long current, long size) {
        Long me = SecurityUtil.currentUserId();
        IPage<Comment> page = commentMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, me)
                        .eq(Comment::getDeleted, 0)
                        .orderByDesc(Comment::getCreateTime));

        return PageResult.of(
                page.getRecords().stream().map(c -> {
                    Post post = postMapper.selectById(c.getPostId());
                    // 帖子被删除，或已改为私密且当前用户不是作者时，不暴露标题
                    boolean visible = post != null
                            && !Integer.valueOf(1).equals(post.getDeleted())
                            && canSeeTitle(post);
                    String title = visible ? post.getTitle() : "（帖子不可见）";
                    return new MyCommentVO(c.getId(), c.getPostId(), title, c.getContent(), c.getCreateTime());
                }).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 私密帖对非作者 / 非管理员等同于不存在，不泄露资源是否存在 */
    private Post requireVisiblePost(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())
                || !Integer.valueOf(1).equals(post.getStatus())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        if (Integer.valueOf(Post.VISIBILITY_PRIVATE).equals(post.getVisibility()) && !canSeeTitle(post)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    private boolean canSeeTitle(Post post) {
        if (!Integer.valueOf(Post.VISIBILITY_PRIVATE).equals(post.getVisibility())) {
            return true;
        }
        Long uid = SecurityUtil.currentUserIdOrNull();
        return uid != null && (uid.equals(post.getUserId()) || SecurityUtil.isAdmin());
    }

    private CommentVO toVO(Comment c) {
        User u = userMapper.selectById(c.getUserId());
        String name = u != null ? u.getNickname() : "未知用户";
        return new CommentVO(c.getId(), c.getPostId(), c.getUserId(), name, c.getParentId(), c.getContent(), c.getCreateTime());
    }
}
