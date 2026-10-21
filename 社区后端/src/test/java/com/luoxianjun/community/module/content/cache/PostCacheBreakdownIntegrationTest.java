package com.luoxianjun.community.module.content.cache;

import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存防击穿集成测试（需要真实 Redis）。
 *
 * <p>核心断言思路：浏览量只在回源时 +1，因此「并发 N 次未命中，DB 浏览量只 +1」
 * 就等价于「N 个请求只回源了 1 次」，无需打桩即可证明防击穿生效。</p>
 */
class PostCacheBreakdownIntegrationTest extends IntegrationTestBase {

    @Test
    void cacheHitDoesNotHitDatabase() {
        Long postId = newPost();
        postCacheService.evict(postId);

        PostVO first = postService.getDetail(postId);
        PostVO second = postService.getDetail(postId);

        assertEquals(1, postMapper.selectById(postId).getViewCount(), "首次未命中应回源并 +1");
        assertEquals(first.viewCount(), second.viewCount(), "命中缓存应直接返回");
        assertEquals(1, postMapper.selectById(postId).getViewCount(), "命中缓存不应再回源");
    }

    @Test
    void concurrentMissLoadsFromDatabaseOnlyOnce() throws Exception {
        Long postId = newPost();
        postCacheService.evict(postId);

        int n = 20;
        AtomicInteger errors = new AtomicInteger();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        postService.getDetail(postId);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(15, TimeUnit.SECONDS), "并发任务应在 15s 内结束");
        }

        assertEquals(0, errors.get(),
                "热点 key 失效的并发请求不应抛异常（旧实现抢不到锁会抛 IllegalStateException）");
        assertEquals(1, postMapper.selectById(postId).getViewCount(),
                "20 个并发未命中只应回源 1 次");
    }

    @Test
    void missingPostIsCachedAsNullMarker() {
        Long missingId = 999_999L;
        redisTemplate.delete("post:detail:" + missingId);

        assertThrows(BizException.class, () -> postService.getDetail(missingId));
        assertEquals("__NULL__", redisTemplate.opsForValue().get("post:detail:" + missingId),
                "不存在的 id 应缓存空标记，防止穿透打 DB");
        assertThrows(BizException.class, () -> postService.getDetail(missingId),
                "再次查询应命中空标记直接失败，不再回源");
    }
}
