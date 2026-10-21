package com.luoxianjun.community.module.content.service;

import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 点赞幂等与并发计数一致性集成测试（需要真实 Redis）。
 *
 * <p>核心不变式：t_post.like_count 必须恒等于 t_post_like 中的真实行数。
 * 这条不变式能同时暴露两类问题——重复点赞产生脏数据，以及并发下计数丢失更新。</p>
 */
class LikeIdempotencyIntegrationTest extends IntegrationTestBase {

    @Test
    void toggleTwiceReturnsToZero() {
        Long postId = newPost();

        postService.toggleLike(postId);
        assertEquals(1, likeRows(postId));
        assertEquals(1, likeCount(postId));

        postService.toggleLike(postId);
        assertEquals(0, likeRows(postId), "再次点赞应取消");
        assertEquals(0, likeCount(postId));
    }

    @Test
    void concurrentLikesFromDifferentUsersKeepCountConsistent() throws Exception {
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
                        postService.toggleLike(postId);
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

        assertEquals(0, errors.get(), "不同用户并发点赞不应失败");
        assertEquals(n, likeRows(postId), "10 个不同用户应产生 10 行点赞记录");
        assertEquals(n, likeCount(postId),
                "并发下 like_count 必须等于实际行数（读-改-写会丢失更新，故改用原子自增）");
    }

    @Test
    void concurrentTogglesFromSameUserNeverDuplicate() throws Exception {
        Long postId = newPost();
        Long uid = createUser();
        int n = 10;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        loginAs(uid);
                        postService.toggleLike(postId);
                    } catch (Exception ignored) {
                        // 同一用户并发点赞会撞唯一索引，事务整体回滚，属预期兜底行为
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(20, TimeUnit.SECONDS));
        }

        assertTrue(likeRows(postId) <= 1, "同一用户对同一帖子最多 1 行，由唯一索引兜底");
        assertEquals(likeRows(postId), likeCount(postId), "点赞计数必须与真实行数一致");
    }
}
