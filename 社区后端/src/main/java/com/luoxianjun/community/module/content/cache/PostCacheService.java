package com.luoxianjun.community.module.content.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luoxianjun.community.module.content.dto.PostVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * 帖子详情缓存（Cache-Aside 模式）。
 * - 命中直接返回；未命中由调用方加分布式锁回源（防击穿）
 * - 不存在的帖子缓存空标记短时间（防穿透）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String KEY_PREFIX = "post:detail:";
    private static final String NULL_MARKER = "__NULL__";
    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration NULL_TTL = Duration.ofMinutes(1);

    public Optional<PostVO> get(Long id) {
        String v = redisTemplate.opsForValue().get(KEY_PREFIX + id);
        if (v == null) {
            return Optional.empty();
        }
        if (NULL_MARKER.equals(v)) {
            return Optional.of(NULL_MARKER_VO); // 命中空标记，视为不存在
        }
        try {
            return Optional.of(objectMapper.readValue(v, PostVO.class));
        } catch (JsonProcessingException e) {
            log.warn("反序列化帖子缓存失败 id={}", id, e);
            return Optional.empty();
        }
    }

    /** 空标记专用哨兵（仅用于区分“缓存中不存在”与“真实不存在”） */
    private static final PostVO NULL_MARKER_VO = new PostVO(
            -1L, null, null, null, null, null, null, null, null, null, null, null);

    public boolean isNullMarker(PostVO vo) {
        return vo != null && vo.id() != null && vo.id() == -1L;
    }

    public void put(PostVO vo) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + vo.id(), objectMapper.writeValueAsString(vo), TTL);
        } catch (JsonProcessingException e) {
            log.warn("序列化帖子缓存失败 id={}", vo.id(), e);
        }
    }

    public void putNull(Long id) {
        redisTemplate.opsForValue().set(KEY_PREFIX + id, NULL_MARKER, NULL_TTL);
    }

    public void evict(Long id) {
        redisTemplate.delete(KEY_PREFIX + id);
    }
}
