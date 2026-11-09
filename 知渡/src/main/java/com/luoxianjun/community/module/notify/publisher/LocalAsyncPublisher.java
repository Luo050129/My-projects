package com.luoxianjun.community.module.notify.publisher;

import com.luoxianjun.community.module.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 本地异步发布器：基于 JDK 21 虚拟线程异步落库。
 * 由 community.mq.type=local 激活（缺省即 local），与 RabbitMQ 实现互换无需改业务代码。
 */
@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "community.mq.type", havingValue = "local", matchIfMissing = true)
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
