package com.luoxianjun.community.module.notify.publisher;

/**
 * 通知发布抽象。业务侧只依赖此接口，不关心底层是本地异步还是 MQ。
 * - local profile：虚拟线程异步落库（见 LocalAsyncPublisher）
 * - prod profile：发往 RabbitMQ，由消费者落库（见 RabbitMqPublisher）
 */
public interface NotificationPublisher {

    void publish(Long receiverId, String type, String content, Long refId);
}
