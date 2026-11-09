package com.luoxianjun.community.module.notify.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoxianjun.community.module.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * community.mq.type=rabbitmq 时的通知消费者：从 MQ 取消息并落库。
 * 落库侧按业务键判重，重复投递不会产生重复通知。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "community.mq.type", havingValue = "rabbitmq")
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
