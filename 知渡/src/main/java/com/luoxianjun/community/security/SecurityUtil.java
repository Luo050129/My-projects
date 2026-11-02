package com.luoxianjun.community.security;

import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 业务侧取当前登录用户：从 SecurityContext 中读取 JWT 鉴权过滤器写入的 userId / role。
 */
public final class SecurityUtil {

    private SecurityUtil() {
    }

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 取当前用户 id，未登录返回 null。
     * <p>
     * 公开接口（帖子列表、详情）需要「登录了就带上自己的视角，没登录也照常能看」，
     * 这类场景不能用会抛异常的 currentUserId()。
     */
    public static Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long)) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(SecurityConstants.ROLE_PREFIX + role));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
}
