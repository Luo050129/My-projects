package com.luoxianjun.community.module.notify.controller;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.module.notify.dto.NotificationVO;
import com.luoxianjun.community.module.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知接口（需登录）。
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Result<PageResult<NotificationVO>> list(@RequestParam(defaultValue = "1") long current,
                                                 @RequestParam(defaultValue = "20") long size) {
        return Result.success(notificationService.listMine(current, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount());
    }

    @PostMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(id);
        return Result.success();
    }

    @PostMapping("/read-all")
    public Result<Void> readAll() {
        notificationService.markAllRead();
        return Result.success();
    }
}
