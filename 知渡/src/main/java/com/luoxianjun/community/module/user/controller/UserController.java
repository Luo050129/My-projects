package com.luoxianjun.community.module.user.controller;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.user.dto.response.UserSummaryVO;
import com.luoxianjun.community.module.user.dto.response.UserVO;
import com.luoxianjun.community.module.user.service.UserService;
import com.luoxianjun.community.module.user.service.UserSocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 用户接口：资料查询、社交关系、运营后台分页（RBAC 方法级鉴权示范）。
 * <p>
 * 注意 {@code /user/me} 与 {@code /user/{id}} 并存：Spring 的路径匹配优先精确字面量，
 * 所以 /user/me 不会落到 {id} 上，无需额外处理。
 */
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserSocialService userSocialService;

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.success(userService.currentUser());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/page")
    public Result<PageResult<UserVO>> page(@RequestParam(defaultValue = "1") long current,
                                           @RequestParam(defaultValue = "10") long size) {
        return Result.success(userService.pageUsers(current, size));
    }

    /** 用户公开摘要：昵称、帖子数、粉丝/关注数、我是否已关注 */
    @GetMapping("/{id}")
    public Result<UserSummaryVO> summary(@PathVariable Long id) {
        return Result.success(userSocialService.summary(id));
    }

    @GetMapping("/{id}/followers")
    public Result<PageResult<UserSummaryVO>> followers(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "20") long size) {
        return Result.success(userSocialService.pageFollowers(id, current, size));
    }

    @GetMapping("/{id}/followees")
    public Result<PageResult<UserSummaryVO>> followees(@PathVariable Long id,
                                                       @RequestParam(defaultValue = "1") long current,
                                                       @RequestParam(defaultValue = "20") long size) {
        return Result.success(userSocialService.pageFollowees(id, current, size));
    }

    @PostMapping("/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        userSocialService.follow(id);
        return Result.success();
    }

    @DeleteMapping("/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        userSocialService.unfollow(id);
        return Result.success();
    }
}
