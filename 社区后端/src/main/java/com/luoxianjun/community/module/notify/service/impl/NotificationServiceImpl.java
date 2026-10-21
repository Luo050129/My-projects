package com.luoxianjun.community.module.notify.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.notify.dto.NotificationVO;
import com.luoxianjun.community.module.notify.entity.Notification;
import com.luoxianjun.community.module.notify.mapper.NotificationMapper;
import com.luoxianjun.community.module.notify.service.NotificationService;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;

    @Override
    @Transactional
    public void save(Long receiverId, String type, String content, Long refId) {
        Notification n = new Notification();
        n.setReceiverId(receiverId);
        n.setType(type);
        n.setContent(content);
        n.setRefId(refId);
        n.setReadStatus(0);
        notificationMapper.insert(n);
    }

    @Override
    public PageResult<NotificationVO> listMine(long current, long size) {
        Long uid = SecurityUtil.currentUserId();
        IPage<Notification> page = notificationMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, uid)
                        .orderByAsc(Notification::getReadStatus)
                        .orderByDesc(Notification::getCreateTime));
        return PageResult.of(
                page.getRecords().stream().map(NotificationVO::from).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        Notification n = notificationMapper.selectById(id);
        if (n == null) {
            throw new BizException(ErrorCode.NOTIFY_NOT_FOUND);
        }
        if (!n.getReceiverId().equals(SecurityUtil.currentUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        n.setReadStatus(1);
        notificationMapper.updateById(n);
    }

    @Override
    @Transactional
    public void markAllRead() {
        Long uid = SecurityUtil.currentUserId();
        notificationMapper.update(
                new Notification(),
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Notification>()
                        .eq(Notification::getReceiverId, uid)
                        .set(Notification::getReadStatus, 1)
                        .eq(Notification::getReadStatus, 0));
    }

    @Override
    public long unreadCount() {
        Long uid = SecurityUtil.currentUserId();
        return notificationMapper.selectCount(
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getReceiverId, uid)
                        .eq(Notification::getReadStatus, 0));
    }
}
