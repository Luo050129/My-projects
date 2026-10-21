package com.luoxianjun.community.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁（SET NX PX + Lua 解锁），用于防止缓存击穿等并发场景。
 * 锁值用 UUID，仅持有者能释放，避免误删他人锁。
 */
@Slf4j
@Component
public class RedisLock {

    private final StringRedisTemplate redisTemplate;

    private static final RedisScript<Long> LOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('set', KEYS[1], ARGV[1], 'NX', 'PX', ARGV[2]) then return 1 else return 0 end",
            Long.class);

    private static final RedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    public RedisLock(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryLock(String key, String value, long expireMillis) {
        Long result = redisTemplate.execute(LOCK_SCRIPT, Collections.singletonList(key),
                value, String.valueOf(expireMillis));
        return Long.valueOf(1).equals(result);
    }

    public boolean unlock(String key, String value) {
        Long result = redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(key), value);
        return Long.valueOf(1).equals(result);
    }

    /**
     * 模板方法：获取锁后执行 action，无论成败都会释放锁。
     */
    public <T> T lock(String key, long expireMillis, Supplier<T> action) {
        String value = UUID.randomUUID().toString();
        boolean locked = tryLock(key, value, expireMillis);
        if (!locked) {
            throw new IllegalStateException("获取分布式锁失败: " + key);
        }
        try {
            return action.get();
        } finally {
            unlock(key, value);
        }
    }

    /**
     * 缓存回源场景专用：抢到锁的线程回源并写缓存；没抢到的线程短暂自旋等待缓存就绪。
     *
     * <p>与 {@link #lock} 的区别：{@link #lock} 抢不到锁会直接抛异常，适合「必须串行」的场景；
     * 而热点 key 失效的瞬间，未抢到锁的请求应该等缓存被回填，而不是报错——否则防击穿反而制造了
     * 一批 500，比直接打 DB 还糟。自旋超时仍未等到缓存时兜底回源，保证可用性优先。</p>
     *
     * @param reader       读缓存，用于双重检查与自旋期间重试
     * @param loader       回源逻辑，仅抢到锁或自旋超时的线程执行
     * @param maxSpin      最大自旋次数
     * @param spinSleepMs  每次自旋间隔（毫秒）
     */
    public <T> T lockWithWait(String key, long expireMillis, Supplier<Optional<T>> reader,
                              Supplier<T> loader, int maxSpin, long spinSleepMs) {
        String value = UUID.randomUUID().toString();
        if (tryLock(key, value, expireMillis)) {
            try {
                Optional<T> recheck = reader.get();
                return recheck.orElseGet(loader);
            } finally {
                unlock(key, value);
            }
        }
        // 未抢到锁：短暂自旋等待其它线程回填缓存
        for (int i = 0; i < maxSpin; i++) {
            sleepQuietly(spinSleepMs);
            Optional<T> cached = reader.get();
            if (cached.isPresent()) {
                return cached.get();
            }
        }
        // 兜底：自旋超时仍未等到缓存则自己回源，可用性优先于「只回源一次」
        return loader.get();
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static String lockKeyOf(String business, Object id) {
        return "lock:" + business + ":" + id;
    }
}
