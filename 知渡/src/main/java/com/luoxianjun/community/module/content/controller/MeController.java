package com.luoxianjun.community.module.content.controller;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.content.dto.MyCommentVO;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.service.CommentService;
import com.luoxianjun.community.module.content.service.PostService;
import com.luoxianjun.community.module.user.dto.response.UserStatsVO;
import com.luoxianjun.community.module.user.service.UserSocialService;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人中心接口，全部以「当前登录用户」为固定主语。
 * <p>
 * 独立成 /me 前缀而不是塞进 /posts、/comments 的查询参数里：
 * 「我的」是权限语义，不该由前端拼 userId 决定看谁的，否则很容易漏做越权校验。
 */
@RestController
@RequestMapping("/me")
@RequiredArgsConstructor
public class MeController {

    private final PostService postService;
    private final CommentService commentService;
    private final UserSocialService userSocialService;

    @GetMapping("/stats")
    public Result<UserStatsVO> stats() {
        return Result.success(userSocialService.myStats());
    }

    /** 我的帖子；默认包含私密帖（这是本人视角，其他人拿不到这条路径） */
    @GetMapping("/posts")
    public Result<PageResult<PostVO>> posts(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(defaultValue = "true") boolean includePrivate) {
        Long me = SecurityUtil.currentUserId();
        return Result.success(postService.pageUserPosts(me, includePrivate, current, size));
    }

    @GetMapping("/comments")
    public Result<PageResult<MyCommentVO>> comments(@RequestParam(defaultValue = "1") long current,
                                                    @RequestParam(defaultValue = "20") long size) {
        return Result.success(commentService.pageMyComments(current, size));
    }

    @GetMapping("/likes")
    public Result<PageResult<PostVO>> likes(@RequestParam(defaultValue = "1") long current,
                                            @RequestParam(defaultValue = "10") long size) {
        Long me = SecurityUtil.currentUserId();
        return Result.success(postService.pageLikedPosts(me, current, size));
    }

    @GetMapping("/favorites")
    public Result<PageResult<PostVO>> favorites(@RequestParam(defaultValue = "1") long current,
                                                @RequestParam(defaultValue = "10") long size) {
        Long me = SecurityUtil.currentUserId();
        return Result.success(postService.pageFavoritedPosts(me, current, size));
    }
}
