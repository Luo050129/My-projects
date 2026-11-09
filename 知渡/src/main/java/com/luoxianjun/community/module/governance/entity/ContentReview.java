package com.luoxianjun.community.module.governance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容审核单。一张单子贯穿「待审 → 通过/驳回 → 申诉 → 申诉裁决」全过程，
 * 所有状态变更另写 t_moderation_log 留痕。
 */
@Data
@TableName("t_content_review")
public class ContentReview {

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_APPEALING = 3;
    public static final int STATUS_APPEAL_APPROVED = 4;
    public static final int STATUS_APPEAL_UPHELD = 5;

    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_AI = "AI";
    public static final String SOURCE_REPORT = "REPORT";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long submitterId;
    /** RULE 规则命中 / AI 模型判定 / REPORT 用户举报 */
    private String source;
    private String reason;
    private Integer status;
    private Long reviewerId;
    private String reviewNote;
    private String appealText;
    private LocalDateTime createTime;
    private LocalDateTime reviewTime;
}
