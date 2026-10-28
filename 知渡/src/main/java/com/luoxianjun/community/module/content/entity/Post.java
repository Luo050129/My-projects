package com.luoxianjun.community.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子。status: 0 草稿 / 1 已发布 / 2 已下架。
 */
@Data
@TableName("t_post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String title;
    private String content;
    private String summary;
    private String tags;
    /** 封面图访问路径，可为空（纯文字帖） */
    private String coverImage;

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
