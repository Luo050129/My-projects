package com.luoxianjun.community.support;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 集成测试门卫：真实 Redis 不可用时跳过相关测试，而不是让整条测试红掉。
 * 这样 CI 上没有 Redis 也能跑通纯单测部分。
 */
public final class RedisAvailability {

    private RedisAvailability() {
    }

    public static boolean available(StringRedisTemplate redisTemplate) {
        if (redisTemplate == null) {
            return false;
        }
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            return false;
        }
    }
}
