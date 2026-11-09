package com.luoxianjun.community.module.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.module.content.dto.request.CommentRequest;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.entity.Comment;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.CommentMapper;
import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 评论链路私密性与计数一致性回归测试。
 *
 * <p>覆盖两处已修缺陷：非作者可对私密帖评论/列评论，以及评论计数「读-改-写」在并发下丢更新。</p>
 */
class CommentVisibilityIntegrationTest extends IntegrationTestBase {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentMapper commentMapper;

    private Long privatePost() {
        return postService.createPost(new CreatePostRequest(
                "私密帖", "只有作者能看见的正文", null, null, "TEXT", 1));
    }

    @Test
    void nonAuthorCannotCommentOnPrivatePost() {
        Long postId = privatePost();
        Long outsider = createUser();
        loginAs(outsider);

        BizException ex = assertThrows(BizException.class, () ->
                commentService.addComment(new CommentRequest(postId, "越权评论", null)));
        assertEquals(ErrorCode.POST_NOT_FOUND.getCode(), ex.getCode(),
                "应返回「帖子不存在」，不泄露资源是否存在");
        assertEquals(0, commentRows(postId), "被拒绝的评论不能落库");
    }

    @Test
    void nonAuthorCannotListCommentsOfPrivatePost() {
        Long postId = privatePost();
        Long outsider = createUser();
        loginAs(outsider);

        assertThrows(BizException.class, () -> commentService.listComments(postId, 1, 10));
    }

    @Test
    void authorCanCommentAndCountIsConsistent() {
        Long postId = privatePost();

        commentService.addComment(new CommentRequest(postId, "作者自己的评论", null));

        assertEquals(1, commentRows(postId));
        assertEquals(1, commentCount(postId));
        assertEquals(1, commentService.listComments(postId, 1, 10).getRecords().size());
    }

    @Test
    void concurrentCommentsKeepCountConsistent() throws Exception {
        Long postId = newPost();
        int n = 10;
        List<Long> userIds = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            userIds.add(createUser());
        }

        AtomicInteger errors = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(n);
            for (Long uid : userIds) {
                executor.submit(() -> {
                    try {
                        start.await();
                        loginAs(uid);
                        commentService.addComment(new CommentRequest(postId, "并发评论", null));
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(20, TimeUnit.SECONDS), "并发任务应在 20s 内结束");
        }

        assertEquals(0, errors.get(), "并发评论不应失败");
        assertEquals(n, commentRows(postId));
        assertEquals(commentRows(postId), commentCount(postId),
                "comment_count 必须恒等于评论表真实行数（读-改-写会丢更新）");
    }

    private int commentRows(Long postId) {
        Long n = commentMapper.selectCount(
                new LambdaQueryWrapper<Comment>().eq(Comment::getPostId, postId));
        return n == null ? 0 : n.intValue();
    }

    private int commentCount(Long postId) {
        Post p = postMapper.selectById(postId);
        return p == null || p.getCommentCount() == null ? -1 : p.getCommentCount();
    }
}
