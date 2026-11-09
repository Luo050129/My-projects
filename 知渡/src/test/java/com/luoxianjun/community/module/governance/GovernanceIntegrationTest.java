package com.luoxianjun.community.module.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.module.content.dto.PostVO;
import com.luoxianjun.community.module.content.dto.request.CreatePostRequest;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.governance.entity.ContentReview;
import com.luoxianjun.community.module.governance.entity.ModerationLog;
import com.luoxianjun.community.module.governance.entity.Report;
import com.luoxianjun.community.module.governance.mapper.ContentReviewMapper;
import com.luoxianjun.community.module.governance.mapper.ModerationLogMapper;
import com.luoxianjun.community.module.governance.mapper.ReportMapper;
import com.luoxianjun.community.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 内容治理闭环集成测试：规则拦截 → 审核 → 申诉 → 举报 → 审计留痕 → 权限。
 * 沿用 test profile（H2 内存库 + 真实 Redis）。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GovernanceIntegrationTest extends IntegrationTestBase {

    @Autowired private GovernanceService governanceService;
    @Autowired private ContentReviewMapper reviewMapper;
    @Autowired private ReportMapper reportMapper;
    @Autowired private ModerationLogMapper logMapper;
    @Autowired private MockMvc mockMvc;

    private Long createPost(String title, String content, int visibility, boolean aiGenerated) {
        return postService.createPost(
                new CreatePostRequest(title, content, null, null, "TEXT", visibility, aiGenerated));
    }

    private ContentReview latestReview(Long postId) {
        return reviewMapper.selectOne(new LambdaQueryWrapper<ContentReview>()
                .eq(ContentReview::getPostId, postId)
                .orderByDesc(ContentReview::getId)
                .last("LIMIT 1"));
    }

    private long auditCount(Long postId, String action) {
        Long n = logMapper.selectCount(new LambdaQueryWrapper<ModerationLog>()
                .eq(ModerationLog::getPostId, postId)
                .eq(ModerationLog::getAction, action));
        return n == null ? 0 : n;
    }

    @Test
    void ruleHitHidesPostFromOthersButNotAuthor() {
        Long author = currentUserId;
        Long postId = createPost("测试赌博内容", "这是一段用于验证规则拦截的正文", 0, false);

        Post post = postMapper.selectById(postId);
        assertEquals(Post.REVIEW_PENDING, post.getReviewStatus());
        ContentReview review = latestReview(postId);
        assertNotNull(review);
        assertEquals(ContentReview.SOURCE_RULE, review.getSource());
        assertEquals(ContentReview.STATUS_PENDING, review.getStatus());
        assertEquals(1, auditCount(postId, "OPEN"));

        // 作者能看到自己的待审帖
        assertEquals(postId, postService.getDetail(postId).id());

        // 他人：详情、列表、评论全部不可见
        Long other = createUser();
        loginAs(other);
        assertThrows(BizException.class, () -> postService.getDetail(postId));
        assertTrue(postService.pagePosts("赌博", 1, 10).getRecords().stream()
                .noneMatch(vo -> postId.equals(vo.id())));
        assertThrows(BizException.class,
                () -> postService.getDetail(postId), "待审帖详情不得被非作者读取");

        loginAs(author);
    }

    @Test
    void approveMakesPostPublicAndRejectThenAppealWorks() {
        Long author = currentUserId;
        Long postId = createPost("待审帖", "含诈骗风险的正文", 0, false);
        ContentReview review = latestReview(postId);

        governanceService.decideReview(review.getId(), "APPROVE", "人工复核通过", createUser());
        assertEquals(Post.REVIEW_APPROVED, postMapper.selectById(postId).getReviewStatus());
        assertEquals(ContentReview.STATUS_APPROVED, reviewMapper.selectById(review.getId()).getStatus());
        assertEquals(1, auditCount(postId, "APPROVE"));

        Long other = createUser();
        loginAs(other);
        assertEquals(postId, postService.getDetail(postId).id());

        // 另一篇：驳回后作者可申诉
        loginAs(author);
        Long rejectedId = createPost("另一篇", "含刷单风险的正文", 0, false);
        ContentReview r2 = latestReview(rejectedId);
        governanceService.decideReview(r2.getId(), "REJECT", "不符合社区规范", createUser());
        assertEquals(Post.REVIEW_REJECTED, postMapper.selectById(rejectedId).getReviewStatus());

        governanceService.appeal(rejectedId, "已删除相关表述，请复核");
        ContentReview appealed = reviewMapper.selectById(r2.getId());
        assertEquals(ContentReview.STATUS_APPEALING, appealed.getStatus());
        assertEquals("已删除相关表述，请复核", appealed.getAppealText());
        assertEquals(1, auditCount(rejectedId, "APPEAL"));

        // 非法状态流转必须被拒
        assertThrows(BizException.class,
                () -> governanceService.decideReview(r2.getId(), "APPROVE", "重复裁决", createUser()));
    }

    @Test
    void aiDeclarationIsPersistedAndExposed() {
        Long postId = createPost("AI 辅助创作说明", "由模型辅助整理的读书笔记", 0, true);
        PostVO vo = postService.getDetail(postId);
        assertTrue(vo.aiGenerated());
        assertEquals(Post.REVIEW_EXEMPT, vo.reviewStatus());

        GovernanceDto.GovernanceVO governance = governanceService.governanceOf(postId);
        assertTrue(governance.aiGenerated());
        assertNotNull(governance.aiDeclaration());
        assertTrue(governance.aiDeclaration().contains("declaredBy="));

        // 未声明的帖子不应带标识
        Long plainId = createPost("纯人工", "手写的一段笔记", 0, false);
        assertFalse(postService.getDetail(plainId).aiGenerated());
    }

    @Test
    void reportFlowAndDuplicateGuard() {
        Long author = currentUserId;
        Long postId = createPost("正常帖子", "一段完全正常的内容", 0, false);
        Long reporter = createUser();

        loginAs(reporter);
        Long reportId = governanceService.submitReport(postId, reporter, "标题与正文不符");
        assertNotNull(reportId);
        assertEquals(Report.STATUS_PENDING, reportMapper.selectById(reportId).getStatus());
        assertThrows(BizException.class,
                () -> governanceService.submitReport(postId, reporter, "再次举报"));
        assertEquals(1, auditCount(postId, "SUBMIT"));

        // 举报会把帖子送进审核队列，但不会自动下架
        assertEquals(Post.REVIEW_PENDING, postMapper.selectById(postId).getReviewStatus());
        assertNotNull(latestReview(postId));

        governanceService.decideReport(reportId, "INVALID", "举报不成立", reporter);
        assertEquals(Report.STATUS_INVALID, reportMapper.selectById(reportId).getStatus());
        assertEquals(1, auditCount(postId, "INVALID"));
        loginAs(author);
    }

    @Test
    void privatePostCannotBeReportedByOthers() {
        Long author = currentUserId;
        Long postId = createPost("私密笔记", "只给自己看的内容", 1, false);
        Long reporter = createUser();
        loginAs(reporter);
        assertThrows(BizException.class,
                () -> governanceService.submitReport(postId, reporter, "想看看私密内容"));
        loginAs(author);
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        mockMvc.perform(get("/admin/reviews").with(user("normal").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/reviews").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }
}
