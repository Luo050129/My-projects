package com.luoxianjun.community.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子。status: 0 草稿 / 1 已发布 / 2 已下架。
 * postType: TEXT 纯文字 / IMAGE 图文；visibility: 0 公开 / 1 私密（仅作者可见）。
 */
@Data
@TableName("t_post")
public class Post {

    public static final String TYPE_TEXT = "TEXT";
    public static final String TYPE_IMAGE = "IMAGE";

    public static final int VISIBILITY_PUBLIC = 0;
    public static final int VISIBILITY_PRIVATE = 1;

    /** 审核状态：0 免审（存量与规则放行）/ 1 待审 / 2 审核通过 / 3 驳回 */
    public static final int REVIEW_EXEMPT = 0;
    public static final int REVIEW_PENDING = 1;
    public static final int REVIEW_APPROVED = 2;
    public static final int REVIEW_REJECTED = 3;

    /** 公开可读的审核状态：待审与驳回的帖子不进入任何公开列表或详情 */
    public static final List<Integer> PUBLIC_READABLE_REVIEW = List.of(REVIEW_EXEMPT, REVIEW_APPROVED);

    public static boolean isPublicReview(Integer reviewStatus) {
        return reviewStatus == null || PUBLIC_READABLE_REVIEW.contains(reviewStatus);
    }

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String summary;
    private String tags;
    /** 封面图访问路径，可为空（纯文字帖） */
    private String coverImage;

    /** 帖子形态：TEXT / IMAGE */
    private String postType;
    /** 可见性：0 公开 / 1 私密 */
    private Integer visibility;

    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    /** 是否经 AI 增强（摘要/标签由 AI 生成） */
    private Integer aiEnhanced;

    /** 显式标识：作者声明该内容由 AI 生成或辅助创作 */
    private Integer aiGenerated;
    /** 审核状态，见 REVIEW_* 常量 */
    private Integer reviewStatus;
    /** 隐式标识：结构化声明元数据（声明人、时间、来源），不上报给前端原文 */
    private String aiDeclaration;

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
