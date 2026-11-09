package com.luoxianjun.community.module.content.service;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.dto.PostSuggestVO;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** 搜索回归：多词包含匹配、正文/标签命中、点赞排序、联想与可见性隔离。 */
class PostSearchIntegrationTest extends IntegrationTestBase {

    private String tag() {
        return "Q" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private Long post(String title, String content, String tags, Integer visibility) {
        return postService.createPost(
                new CreatePostRequest(title, content, tags, null, "TEXT", visibility));
    }

    private void setLikes(Long id, int n) {
        Post p = postMapper.selectById(id);
        p.setLikeCount(n);
        postMapper.updateById(p);
    }

    private Set<Long> ids(PageResult<PostVO> page) {
        return page.getRecords().stream().map(PostVO::id).collect(Collectors.toSet());
    }

    @Test
    void multipleWordsMatchAnyWord() {
        String t = tag();
        Long a = post("阿尔法" + t, "正文一", null, 0);
        Long b = post("贝塔" + t, "正文二", null, 0);
        Long c = post("无关标题", "正文三", null, 0);

        Set<Long> found = ids(postService.pagePosts("阿尔法" + t + " 贝塔" + t, 1, 20));
        assertTrue(found.contains(a), "第一个词应命中");
        assertTrue(found.contains(b), "第二个词也应命中（任一词命中即可）");
        assertFalse(found.contains(c), "不含任何词的帖子不应出现");
    }

    @Test
    void contentAndTagsAreSearchableAndCaseInsensitive() {
        String t = tag();
        String bodyWord = "BODY" + t;
        String tagWord = "TAGX" + t;
        Long a = post("普通标题", "正文里藏着 " + bodyWord, "普通标签", 0);
        Long b = post("另一个标题", "正文普通", "标签里藏着" + tagWord, 0);

        assertTrue(ids(postService.pagePosts(bodyWord, 1, 20)).contains(a), "正文命中");
        assertTrue(ids(postService.pagePosts(tagWord, 1, 20)).contains(b), "标签命中");
        // 小写输入也要能命中大写内容：H2 与 MySQL 必须表现一致
        assertTrue(ids(postService.pagePosts(bodyWord.toLowerCase(Locale.ROOT), 1, 20)).contains(a));
    }

    @Test
    void searchResultsOrderedByLikeCount() {
        String t = tag();
        Long low = post("SORT" + t + "甲", "正文", null, 0);
        Long high = post("SORT" + t + "乙", "正文", null, 0);
        Long mid = post("SORT" + t + "丙", "正文", null, 0);
        setLikes(low, 1);
        setLikes(high, 9);
        setLikes(mid, 5);

        List<Long> ordered = postService.pagePosts("SORT" + t, 1, 20)
                .getRecords().stream().map(PostVO::id).toList();
        assertEquals(List.of(high, mid, low), ordered, "搜索结果必须按点赞数降序");
    }

    @Test
    void suggestReturnsTopByLikesAndHidesNonPublicPosts() {
        String t = tag();
        Long a = post("SUG" + t + "甲", "摘要甲", null, 0);
        Long b = post("SUG" + t + "乙", "正文里也提到了 SUG" + t + " 这个词", null, 0);
        Long hidden = post("SUG" + t + "私密", "不可见", null, 1);
        setLikes(a, 2);
        setLikes(b, 11);

        List<PostSuggestVO> suggestions = postService.suggest("SUG" + t, 2);
        assertEquals(2, suggestions.size());
        assertEquals(b, suggestions.get(0).id(), "联想同样按点赞数降序");
        assertEquals(a, suggestions.get(1).id());
        assertTrue(suggestions.stream().noneMatch(s -> s.id().equals(hidden)), "私密帖不得出现在联想里");
        assertTrue(suggestions.stream().allMatch(s -> s.matchedWord() != null));
        assertFalse(suggestions.get(0).snippet().isBlank(), "应带命中上下文");

        assertFalse(ids(postService.pagePosts("SUG" + t, 1, 20)).contains(hidden));
    }

    @Test
    void emptyKeywordKeepsTimeFlowAndNoSuggestions() {
        assertTrue(postService.suggest(null, 8).isEmpty());
        assertTrue(postService.suggest("   ", 8).isEmpty());
        assertTrue(postService.suggest("% _", 8).isEmpty(), "纯符号不应变成全表匹配");
        assertTrue(postService.pagePosts(null, 1, 10).getTotal() > 0, "空关键词仍返回时间流");
    }

    @Test
    void pendingReviewPostIsNotSearchable() {
        String t = tag();
        Long id = post("AUDIT" + t, "正文", null, 0);
        Post p = postMapper.selectById(id);
        p.setReviewStatus(Post.REVIEW_PENDING);
        postMapper.updateById(p);

        assertFalse(ids(postService.pagePosts("AUDIT" + t, 1, 20)).contains(id));
        assertTrue(postService.suggest("AUDIT" + t, 8).isEmpty());
    }
}
