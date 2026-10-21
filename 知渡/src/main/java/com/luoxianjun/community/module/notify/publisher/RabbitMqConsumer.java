package com.luoxianjun.community.module.notify.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoxianjun.community.module.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * prod 环境下的通知消费者：从 MQ 取消息并落库（含失败重试由 MQ 重试机制保证）。
 */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class RabbitMqConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.QUEUE)
    public void consume(String json) {
        try {
            NotificationMessage msg = objectMapper.readValue(json, NotificationMessage.class);
            notificationService.save(msg.receiverId(), msg.type(), msg.content(), msg.refId());
        } catch (Exception e) {
            log.error("通知消费失败 payload={}", json, e);
            throw new RuntimeException("通知消费失败，触发 MQ 重试", e);
        }
    }
}
