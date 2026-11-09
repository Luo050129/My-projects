package com.luoxianjun.community.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 治理审计日志：记录「谁在何时把哪个业务对象从什么状态改到什么状态」。
 * 只追加不修改，便于事后追溯与申诉复核。
 */
@Data
@TableName("t_moderation_log")
public class ModerationLog {

    public static final String BIZ_REVIEW = "REVIEW";
    public static final String BIZ_REPORT = "REPORT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;
    private Long bizId;
    private Long postId;
    private String action;
    private Long operatorId;
    private Integer fromStatus;
    private Integer toStatus;
    private String note;
    private LocalDateTime createTime;
}
