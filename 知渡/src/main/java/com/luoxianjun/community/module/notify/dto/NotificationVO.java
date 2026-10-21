package com.luoxianjun.community.module.notify.dto;

import java.time.LocalDateTime;

/**
 * 通知视图对象。
 */
public record NotificationVO(Long id, String type, String content, Long refId,
                             Integer readStatus, LocalDateTime createTime) {

    public static NotificationVO from(com.luoxianjun.community.module.notify.entity.Notification n) {
        return new NotificationVO(n.getId(), n.getType(), n.getContent(), n.getRefId(),
                n.getReadStatus(), n.getCreateTime());
    }
}
