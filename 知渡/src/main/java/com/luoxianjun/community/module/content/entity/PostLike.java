package com.luoxianjun.community.module.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞记录。唯一索引 (post_id, user_id) 保证一人一赞（幂等）。
 */
@Data
@TableName("t_post_like")
public class PostLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;
    private Long userId;

    private LocalDateTime createTime;
}
