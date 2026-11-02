package com.luoxianjun.community.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.entity.UserFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserFollowMapper extends BaseMapper<UserFollow> {

    /** 某人的粉丝：关注了 userId 的人，按关注时间倒序 */
    @Select("""
            SELECT u.* FROM t_user u
            JOIN t_user_follow f ON f.follower_id = u.id
            WHERE f.followee_id = #{userId} AND u.deleted = 0
            ORDER BY f.create_time DESC
            """)
    IPage<User> pageFollowers(IPage<User> page, @Param("userId") Long userId);

    /** 某人关注的人，按关注时间倒序 */
    @Select("""
            SELECT u.* FROM t_user u
            JOIN t_user_follow f ON f.followee_id = u.id
            WHERE f.follower_id = #{userId} AND u.deleted = 0
            ORDER BY f.create_time DESC
            """)
    IPage<User> pageFollowees(IPage<User> page, @Param("userId") Long userId);
}
