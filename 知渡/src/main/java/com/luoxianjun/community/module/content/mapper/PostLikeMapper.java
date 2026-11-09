package com.luoxianjun.community.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {

    /**
     * 我点赞过的帖子，按点赞时间倒序。
     * <p>
     * 用 IPage 作为首参，分页插件会自动拼 LIMIT 并生成 count 语句；
     * 排序取 t_post_like.create_time 而不是帖子创建时间，才符合「最近赞过」的直觉。
     */
    @Select("""
            SELECT p.* FROM t_post p
            JOIN t_post_like l ON l.post_id = p.id
            WHERE l.user_id = #{userId}
              AND p.deleted = 0 AND p.status = 1
              AND (p.visibility = 0 OR p.user_id = #{userId})
              AND p.review_status IN (0, 2)
            ORDER BY l.create_time DESC
            """)
    IPage<Post> pageLikedByUser(IPage<Post> page, @Param("userId") Long userId);
}
