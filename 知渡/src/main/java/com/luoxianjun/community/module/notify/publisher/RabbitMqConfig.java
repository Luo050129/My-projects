package com.luoxianjun.community.module.notify.publisher;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * community.mq.type=rabbitmq 时声明通知交换机/队列/绑定。本地不加载，避免连接 RabbitMQ。
 */
@Configuration
@ConditionalOnProperty(name = "community.mq.type", havingValue = "rabbitmq")
public class RabbitMqConfig {

    public static final String QUEUE = "community.notification.queue";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(RabbitMqPublisher.EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(RabbitMqPublisher.ROUTING_KEY);
    }
}
