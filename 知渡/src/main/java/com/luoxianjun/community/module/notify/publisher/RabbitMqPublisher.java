package com.luoxianjun.community.module.notify.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 生产环境发布器：将通知发往 RabbitMQ（解耦 + 削峰），由消费者落库。
 * 仅 prod profile 生效；本地用 {@link LocalAsyncPublisher} 代替。
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class RabbitMqPublisher implements NotificationPublisher {

    public static final String EXCHANGE = "community.notification.exchange";
    public static final String ROUTING_KEY = "notification.create";

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(Long receiverId, String type, String content, Long refId) {
        try {
            String json = objectMapper.writeValueAsString(new NotificationMessage(receiverId, type, content, refId));
            rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY, json);
        } catch (Exception e) {
            log.error("通知发送至 MQ 失败 receiverId={} type={}", receiverId, type, e);
        }
    }
}
