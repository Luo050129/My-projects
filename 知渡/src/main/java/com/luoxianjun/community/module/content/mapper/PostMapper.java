package com.luoxianjun.community.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luoxianjun.community.module.content.entity.Post;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 某人的帖子累计获赞。
     * <p>
     * 用 SQL 聚合而不是把帖子全查出来在内存里加：个人中心每次都要这个数，
     * 帖子多了以后一条 SUM 和 N 条 selectList 的差距很难看。
     */
    @Select("SELECT COALESCE(SUM(like_count), 0) FROM t_post WHERE user_id = #{userId} AND deleted = 0")
    long sumLikesByUser(@Param("userId") Long userId);
}
