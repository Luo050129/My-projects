package com.luoxianjun.community.module.user.controller;

import com.luoxianjun.community.common.annotation.RateLimit;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.user.dto.request.LoginRequest;
import com.luoxianjun.community.module.user.dto.request.RegisterRequest;
import com.luoxianjun.community.module.user.dto.response.LoginResponse;
import com.luoxianjun.community.module.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口（公开）。
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return Result.success();
    }

    @RateLimit(key = "login", window = 60, max = 10)
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }
}
