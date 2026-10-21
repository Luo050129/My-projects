package com.luoxianjun.community.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.dto.CommentVO;
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

    @Override
    @Transactional
    public Long addComment(CommentRequest request) {
        Post post = postMapper.selectById(request.postId());
        if (post == null || post.getDeleted() == 1 || post.getStatus() != 1) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        Long uid = SecurityUtil.currentUserId();
        Comment comment = new Comment();
        comment.setPostId(request.postId());
        comment.setUserId(uid);
        comment.setParentId(request.parentId() == null ? 0L : request.parentId());
        comment.setContent(request.content());
        comment.setStatus(1);
        commentMapper.insert(comment);

        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        // 异步解耦：评论非作者本人时通知帖主（不阻塞主流程）
        if (!post.getUserId().equals(uid)) {
            User author = userMapper.selectById(uid);
            String name = author != null ? author.getNickname() : "有人";
            notificationPublisher.publish(post.getUserId(), "COMMENT",
                    name + " 评论了你的帖子《" + post.getTitle() + "》", post.getId());
        }
        return comment.getId();
    }

    @Override
    public PageResult<CommentVO> listComments(Long postId, long current, long size) {
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

    private CommentVO toVO(Comment c) {
        User u = userMapper.selectById(c.getUserId());
        String name = u != null ? u.getNickname() : "未知用户";
        return new CommentVO(c.getId(), c.getPostId(), c.getUserId(), name, c.getParentId(), c.getContent(), c.getCreateTime());
    }
}
