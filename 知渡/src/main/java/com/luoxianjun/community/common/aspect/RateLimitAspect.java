package com.luoxianjun.community.common.aspect;

import com.luoxianjun.community.common.annotation.RateLimit;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

/**
 * 限流切面：固定窗口计数器。命中限流返回 429 风格的业务错误。
 *
 * <p>INCR 与 EXPIRE 放进同一段 Lua 保证原子性——分两步时 expire 一旦失败，
 * 会留下永不过期的 key，该来源会被永久限流。Redis 不可用时 fail-open，
 * 不能让限流组件把登录 / 上传整体拖成 500。</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final DefaultRedisScript<Long> RATE_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) " +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end " +
            "return c", Long.class);

    private final StringRedisTemplate redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return pjp.proceed();
        }
        HttpServletRequest request = attrs.getRequest();
        // 刻意不读 X-Forwarded-For：该头可伪造，直接信任等于允许绕过限流。
        // 经可信代理部署时，应由网关把真实 IP 写入 remoteAddr 或改用网关限流。
        String ip = request.getRemoteAddr();
        String redisKey = "ratelimit:" + rateLimit.key() + ":" + ip;

        Long count;
        try {
            count = redisTemplate.execute(RATE_LIMIT_SCRIPT,
                    List.of(redisKey), String.valueOf(rateLimit.window()));
        } catch (Exception e) {
            log.warn("限流组件不可用，本次放行 key={}", redisKey, e);
            return pjp.proceed();
        }
        if (count != null && count > rateLimit.max()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "请求过于频繁，请稍后再试");
        }
        return pjp.proceed();
    }
}
