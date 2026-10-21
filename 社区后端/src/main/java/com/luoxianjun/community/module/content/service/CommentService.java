package com.luoxianjun.community.module.content.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.dto.CommentVO;
import com.luoxianjun.community.module.content.dto.request.CommentRequest;

/**
 * 评论服务。
 */
public interface CommentService {

    /** 发表评论：入库 + 帖子评论数 + 异步通知作者 */
    Long addComment(CommentRequest request);

    /** 帖子评论列表（一级评论与回复混合，按时间正序） */
    PageResult<CommentVO> listComments(Long postId, long current, long size);
}
