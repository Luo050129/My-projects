package com.luoxianjun.community.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注关系。follower 主动关注 followee。
 * 唯一索引 (follower_id, followee_id) 保证重复关注幂等。
 */
@Data
@TableName("t_user_follow")
public class UserFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注发起者 */
    private Long followerId;

    /** 被关注者 */
    private Long followeeId;

    private LocalDateTime createTime;
}
