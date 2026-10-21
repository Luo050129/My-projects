package com.luoxianjun.community.module.content.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.dto.request.UpdatePostRequest;

/**
 * 帖子服务。
 */
public interface PostService {

    /** 发布帖子，返回帖子ID */
    Long createPost(CreatePostRequest request);

    /** 更新（仅作者或管理员） */
    void updatePost(Long id, UpdatePostRequest request);

    /** 删除（逻辑删除，仅作者或管理员） */
    void deletePost(Long id);

    /** 帖子详情：Cache-Aside + 分布式锁防击穿 + 空值防穿透 */
    PostVO getDetail(Long id);

    /** 帖子分页（已发布，按时间倒序，支持标题关键字） */
    PageResult<PostVO> pagePosts(String keyword, long current, long size);

    /** 点赞/取消点赞（幂等：唯一索引保证一人一赞） */
    void toggleLike(Long id);

    /** AI 增强结果回写（摘要/标签） */
    void applyAiEnhancement(Long postId, String summary, String tags);
}
