package com.luoxianjun.community.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子收藏。唯一索引 (post_id, user_id) 保证一人一收藏。
 */
@Data
@TableName("t_post_favorite")
public class PostFavorite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;

    private LocalDateTime createTime;
}
