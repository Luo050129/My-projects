package com.luoxianjun.community.module.user.dto.response;

/**
 * 登录返回：token + 基本信息。
 */
public record LoginResponse(Long userId, String nickname, String role, String token) {
}
