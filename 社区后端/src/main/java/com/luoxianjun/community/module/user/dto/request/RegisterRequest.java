package com.luoxianjun.community.module.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 */
public record RegisterRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 32, message = "用户名长度 3-32")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度 6-64")
        String password,

        @NotBlank(message = "昵称不能为空")
        @Size(max = 32, message = "昵称过长")
        String nickname
) {
}
