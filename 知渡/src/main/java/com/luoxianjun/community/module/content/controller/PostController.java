package com.luoxianjun.community.module.content.controller;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.content.dto.PostInteractionVO;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.dto.request.UpdatePostRequest;
import com.luoxianjun.community.module.content.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子接口。读公开，写需登录。
 */
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreatePostRequest request) {
        return Result.success(postService.createPost(request));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest request) {
        postService.updatePost(id, request);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.success(postService.getDetail(id));
    }

    @GetMapping
    public Result<PageResult<PostVO>> page(@RequestParam(required = false) String keyword,
                                          @RequestParam(defaultValue = "1") long current,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.success(postService.pagePosts(keyword, current, size));
    }

    @PostMapping("/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        postService.toggleLike(id);
        return Result.success();
    }

    /** 收藏 / 取消收藏，返回切换后的状态：true = 已收藏 */
    @PostMapping("/{id}/favorite")
    public Result<Boolean> favorite(@PathVariable Long id) {
        return Result.success(postService.toggleFavorite(id));
    }

    /** 当前用户与该帖的交互状态（是否已赞 / 已收藏），未登录返回全 false */
    @GetMapping("/{id}/interaction")
    public Result<PostInteractionVO> interaction(@PathVariable Long id) {
        return Result.success(postService.interaction(id));
    }
}
