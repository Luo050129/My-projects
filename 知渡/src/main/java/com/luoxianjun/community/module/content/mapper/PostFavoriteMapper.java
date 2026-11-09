package com.luoxianjun.community.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.entity.PostFavorite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostFavoriteMapper extends BaseMapper<PostFavorite> {

    /** 我收藏的帖子，按收藏时间倒序 */
    @Select("""
            SELECT p.* FROM t_post p
            JOIN t_post_favorite f ON f.post_id = p.id
            WHERE f.user_id = #{userId}
              AND p.deleted = 0 AND p.status = 1
              AND (p.visibility = 0 OR p.user_id = #{userId})
            ORDER BY f.create_time DESC
            """)
    IPage<Post> pageFavoritedByUser(IPage<Post> page, @Param("userId") Long userId);
}
