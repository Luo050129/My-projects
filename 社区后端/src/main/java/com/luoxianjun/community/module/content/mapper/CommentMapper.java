package com.luoxianjun.community.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.luoxianjun.community.module.content.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
