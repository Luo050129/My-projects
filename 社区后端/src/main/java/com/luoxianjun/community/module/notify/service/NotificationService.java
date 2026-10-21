package com.luoxianjun.community.module.notify.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.notify.dto.NotificationVO;

/**
 * 通知服务。
 */
public interface NotificationService {

    /** 落库一条通知（由发布器调用） */
    void save(Long receiverId, String type, String content, Long refId);

    /** 当前用户通知列表（未读优先） */
    PageResult<NotificationVO> listMine(long current, long size);

    /** 标记已读（校验归属） */
    void markRead(Long id);

    /** 全部已读 */
    void markAllRead();

    /** 当前用户未读数量 */
    long unreadCount();
}
