package com.luoxianjun.community.module.content.controller;

import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.content.dto.PostSuggestVO;
import com.luoxianjun.community.module.content.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 搜索接口。独立于 /posts/{id}，避免路径变量与精确路径混淆。
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final PostService postService;

    /** 输入联想：多词包含匹配，按点赞数降序；空关键词直接返回空列表 */
    @GetMapping("/suggest")
    public Result<List<PostSuggestVO>> suggest(@RequestParam(required = false) String keyword,
                                               @RequestParam(defaultValue = "8") int limit) {
        return Result.success(postService.suggest(keyword, limit));
    }
}
