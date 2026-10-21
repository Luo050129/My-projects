package com.luoxianjun.community.common.util;

import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 分布式锁集成测试（需要真实 Redis）。
 * 验证互斥性、防误删他人锁、自动过期防死锁，以及缓存回源自旋等待场景。
 */
class RedisLockIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RedisLock redisLock;

    @Test
    void lockIsMutuallyExclusive() {
        String key = "lock:it:" + UUID.randomUUID();
        String owner = UUID.randomUUID().toString();

        assertTrue(redisLock.tryLock(key, owner, 5000), "首个线程应抢到锁");
        assertFalse(redisLock.tryLock(key, UUID.randomUUID().toString(), 5000), "锁必须互斥");

        assertTrue(redisLock.unlock(key, owner), "持有者应能释放");
        assertNull(redisTemplate.opsForValue().get(key), "释放后锁应不存在");
    }

    @Test
    void cannotReleaseOthersLock() {
        String key = "lock:it:" + UUID.randomUUID();
        String owner = UUID.randomUUID().toString();
        assertTrue(redisLock.tryLock(key, owner, 5000));

        assertFalse(redisLock.unlock(key, UUID.randomUUID().toString()), "不能释放他人的锁");
        assertNotNull(redisTemplate.opsForValue().get(key), "他人的锁必须仍然存在");

        assertTrue(redisLock.unlock(key, owner));
    }

    @Test
    void lockExpiresAutomatically() throws Exception {
        String key = "lock:it:" + UUID.randomUUID();
        assertTrue(redisLock.tryLock(key, UUID.randomUUID().toString(), 300));

        Thread.sleep(600);

        assertTrue(redisLock.tryLock(key, UUID.randomUUID().toString(), 5000),
                "锁过期后应能重新抢到，避免业务异常时永久死锁");
        redisTemplate.delete(key);
    }

    /**
     * 缓存回源场景：热点 key 失效瞬间大量并发涌入，
     * 未抢到锁的线程应自旋等缓存回填，而不是抛异常。
     */
    @Test
    void concurrentWaitersDoNotThrowAndOnlyOneLoads() throws Exception {
        String key = "lock:it:wait:" + UUID.randomUUID();
        Map<String, String> fakeCache = new ConcurrentHashMap<>();
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger errors = new AtomicInteger();
        int n = 20;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(n);
            for (int i = 0; i < n; i++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        String value = redisLock.lockWithWait(key, 3000,
                                () -> Optional.ofNullable(fakeCache.get(key)),
                                () -> {
                                    loads.incrementAndGet();
                                    String loaded = "v-" + UUID.randomUUID();
                                    fakeCache.put(key, loaded);
                                    return loaded;
                                }, 100, 20);
                        assertNotNull(value);
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "并发任务应在 10s 内结束");
        }

        assertEquals(0, errors.get(), "未抢到锁的线程应自旋等缓存，而不是抛异常");
        assertEquals(1, loads.get(), "20 个并发线程只应有 1 个真正回源");
    }
}
