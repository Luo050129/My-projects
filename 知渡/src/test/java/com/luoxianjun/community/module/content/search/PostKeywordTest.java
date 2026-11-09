package com.luoxianjun.community.module.content.search;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 搜索词归一化：纯单测，离线可跑。 */
class PostKeywordTest {

    @Test
    void splitsByWhitespaceAndChinesePunctuation() {
        assertEquals(List.of("Redis", "缓存", "击穿"), PostKeyword.split("Redis 缓存，击穿"));
        assertEquals(List.of("锁", "过期", "并发"), PostKeyword.split("锁、过期;并发"));
    }

    @Test
    void deduplicatesAndCapsWordCount() {
        assertEquals(List.of("缓存", "击穿"), PostKeyword.split("缓存 缓存 击穿"));
        assertEquals(PostKeyword.MAX_WORDS, PostKeyword.split("甲 乙 丙 丁 戊 己 庚").size());
    }

    @Test
    void stripsLikeWildcardsAndKeepsOnlyLettersDigits() {
        assertEquals(List.of("abc123"), PostKeyword.split("a%b_c123"));
        assertEquals(List.of("缓存"), PostKeyword.split("「缓存」!!!"));
        assertTrue(PostKeyword.split("% _ ' OR 1=1 --").stream().noneMatch(w -> w.contains("%") || w.contains("_")));
    }

    @Test
    void blankReturnsEmpty() {
        assertTrue(PostKeyword.split(null).isEmpty());
        assertTrue(PostKeyword.split("   ").isEmpty());
        assertTrue(PostKeyword.split("，、；").isEmpty());
    }

    @Test
    void capsInputAndWordLength() {
        // 总长仍在 200 以内，验证的是「单词」截断而不是整串截断
        List<String> words = PostKeyword.split("甲".repeat(60) + " " + "乙".repeat(60));
        assertEquals(2, words.size());
        assertEquals(PostKeyword.MAX_WORD_LENGTH, words.get(0).length());
    }
}
