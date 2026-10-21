package com.luoxianjun.community.module.notify.publisher;

import com.luoxianjun.community.module.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 本地异步发布器：基于 JDK 21 虚拟线程异步落库。
 * 与 RabbitMQ 实现互换无需改动业务代码（依赖倒置）。
 */
@Slf4j
@Service
@Primary
@Profile({"local", "default", "!prod"})
@RequiredArgsConstructor
public class LocalAsyncPublisher implements NotificationPublisher {

    private final NotificationService notificationService;

    @Async("asyncVirtualExecutor")
    @Override
    public void publish(Long receiverId, String type, String content, Long refId) {
        try {
            notificationService.save(receiverId, type, content, refId);
        } catch (Exception e) {
            log.error("本地异步通知落库失败 receiverId={} type={}", receiverId, type, e);
        }
    }
}
