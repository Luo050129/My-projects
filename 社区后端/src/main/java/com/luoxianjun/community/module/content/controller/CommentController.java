package com.luoxianjun.community.module.content.controller;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.content.dto.CommentVO;
import com.luoxianjun.community.module.content.dto.request.CommentRequest;
import com.luoxianjun.community.module.content.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论接口。
 */
@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public Result<Long> add(@Valid @RequestBody CommentRequest request) {
        return Result.success(commentService.addComment(request));
    }

    @GetMapping("/posts/{postId}")
    public Result<PageResult<CommentVO>> list(@PathVariable Long postId,
                                             @RequestParam(defaultValue = "1") long current,
                                             @RequestParam(defaultValue = "50") long size) {
        return Result.success(commentService.listComments(postId, current, size));
    }
}
