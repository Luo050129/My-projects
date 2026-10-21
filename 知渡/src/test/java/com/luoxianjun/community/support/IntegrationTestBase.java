package com.luoxianjun.community.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.entity.PostLike;
import com.luoxianjun.community.module.content.mapper.PostLikeMapper;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.content.service.PostService;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 需要真实 Redis 的集成测试基类。
 *
 * <p>使用 test profile：H2 内存库 + 本机 Redis(db=1)，既不污染本地 data/ 文件库，
 * 又能验证分布式锁、缓存防击穿、并发幂等这些「单测 mock 不出来」的行为。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    @Autowired
    protected StringRedisTemplate redisTemplate;

    @Autowired
    protected UserMapper userMapper;

    @Autowired
    protected PostMapper postMapper;

    @Autowired
    protected PostLikeMapper postLikeMapper;

    @Autowired
    protected PostService postService;

    @Autowired
    protected PostCacheService postCacheService;

    protected Long currentUserId;

    @BeforeEach
    void requireRedisAndLogin() {
        assumeTrue(RedisAvailability.available(redisTemplate), "Redis 不可用，跳过需要真实 Redis 的集成测试");
        currentUserId = createUser();
        loginAs(currentUserId);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    protected Long createUser() {
        User u = new User();
        u.setUsername("u_" + UUID.randomUUID().toString().substring(0, 8));
        u.setPassword("test-pwd");
        u.setNickname("测试用户");
        u.setRole("USER");
        u.setStatus(1);
        u.setDeleted(0);
        userMapper.insert(u);
        return u.getId();
    }

    /**
     * 业务侧从 SecurityContext 取 userId，而 SecurityContextHolder 是线程隔离的，
     * 因此并发场景必须在各自线程内单独设置。
     */
    protected void loginAs(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    protected Long newPost() {
        return postService.createPost(
                new CreatePostRequest("并发测试帖", "用于验证缓存防击穿与点赞幂等的正文", null));
    }

    protected int likeRows(Long postId) {
        Long n = postLikeMapper.selectCount(
                new LambdaQueryWrapper<PostLike>().eq(PostLike::getPostId, postId));
        return n == null ? 0 : n.intValue();
    }

    protected int likeCount(Long postId) {
        Post p = postMapper.selectById(postId);
        return p == null ? -1 : p.getLikeCount();
    }
}
