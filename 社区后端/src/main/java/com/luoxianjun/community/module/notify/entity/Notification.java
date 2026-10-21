package com.luoxianjun.community.module.notify.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站内通知。type 示例：POST_PUBLISHED / COMMENT / LIKE / SYSTEM。
 */
@Data
@TableName("t_notification")
public class Notification {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long receiverId;
    private String type;
    private String content;
    private Long refId;
    /** 0 未读 1 已读 */
    private Integer readStatus;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
