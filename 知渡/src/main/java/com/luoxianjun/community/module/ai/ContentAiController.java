package com.luoxianjun.community.module.ai;

import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.ai.dto.DeepSeekDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 能力接口：内容增强（需登录）。
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class ContentAiController {

    private final ContentAiService contentAiService;

    @PostMapping("/posts/{id}/enhance")
    public Result<DeepSeekDto.AiResult> enhance(@PathVariable Long id) {
        return Result.success(contentAiService.enhancePost(id));
    }
}
