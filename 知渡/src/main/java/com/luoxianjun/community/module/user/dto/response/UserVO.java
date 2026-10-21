package com.luoxianjun.community.module.user.dto.response;

import com.luoxianjun.community.module.user.entity.User;

import java.time.LocalDateTime;

/**
 * 用户视图对象，对外不暴露密码等敏感字段。
 */
public record UserVO(Long id, String username, String nickname, String role, Integer status, LocalDateTime createTime) {

    public static UserVO from(User u) {
        return new UserVO(u.getId(), u.getUsername(), u.getNickname(), u.getRole(), u.getStatus(), u.getCreateTime());
    }
}
