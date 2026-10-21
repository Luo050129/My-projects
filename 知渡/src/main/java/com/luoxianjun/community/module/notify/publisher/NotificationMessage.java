package com.luoxianjun.community.module.notify.publisher;

/**
 * 跨进程通知消息载体（RabbitMQ 传输对象）。
 */
public record NotificationMessage(Long receiverId, String type, String content, Long refId) {
}
