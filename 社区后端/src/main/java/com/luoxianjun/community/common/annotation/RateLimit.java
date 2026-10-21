package com.luoxianjun.community.common.annotation;

import java.lang.annotation.*;

/**
 * 接口级限流注解（固定窗口计数，基于 Redis）。仅做示范：登录等高频接口可挂此注解。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    /** 时间窗大小（秒） */
    int window() default 60;

    /** 时间窗内允许的最大请求数 */
    int max() default 20;

    /** 限流维度前缀 */
    String key() default "rl";
}
