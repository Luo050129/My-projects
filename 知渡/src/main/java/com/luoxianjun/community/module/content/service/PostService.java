package com.luoxianjun.community.module.content.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.dto.PostInteractionVO;
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

    /** 帖子详情：Cache-Aside + 分布式锁防击穿 + 空值防穿透；私密帖仅作者可见 */
    PostVO getDetail(Long id);

    /** 公开帖子分页（只含公开帖，按时间倒序，支持标题关键字） */
    PageResult<PostVO> pagePosts(String keyword, long current, long size);

    /** 某人的帖子分页；includePrivate 为 true 时包含私密帖，调用方必须保证这是本人视角 */
    PageResult<PostVO> pageUserPosts(Long userId, boolean includePrivate, long current, long size);

    /** 某人点赞过的帖子（按点赞时间倒序） */
    PageResult<PostVO> pageLikedPosts(Long userId, long current, long size);

    /** 某人收藏的帖子（按收藏时间倒序） */
    PageResult<PostVO> pageFavoritedPosts(Long userId, long current, long size);

    /** 点赞/取消点赞（幂等：唯一索引保证一人一赞） */
    void toggleLike(Long id);

    /** 收藏/取消收藏，返回切换后的状态：true 表示当前已收藏 */
    boolean toggleFavorite(Long id);

    /** 当前用户与该帖的交互状态；未登录返回全 false */
    PostInteractionVO interaction(Long id);

    /** 某人发布的帖子数 */
    long countPostsByUser(Long userId, boolean includePrivate);

    /** 某人的帖子累计获赞数 */
    long sumLikesReceived(Long userId);

    /** AI 增强结果回写（摘要/标签） */
    void applyAiEnhancement(Long postId, String summary, String tags);
}
