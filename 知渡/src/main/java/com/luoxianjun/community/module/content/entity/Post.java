package com.luoxianjun.community.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

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

    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
