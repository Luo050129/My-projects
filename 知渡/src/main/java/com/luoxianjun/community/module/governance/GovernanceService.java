package com.luoxianjun.community.module.governance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.luoxianjun.community.common.exception.BizException;
import com.luoxianjun.community.common.exception.ErrorCode;
import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.module.content.cache.PostCacheService;
import com.luoxianjun.community.module.content.entity.Post;
import com.luoxianjun.community.module.content.mapper.PostMapper;
import com.luoxianjun.community.module.governance.entity.ContentReview;
import com.luoxianjun.community.module.governance.entity.ModerationLog;
import com.luoxianjun.community.module.governance.entity.Report;
import com.luoxianjun.community.module.governance.mapper.ContentReviewMapper;
import com.luoxianjun.community.module.governance.mapper.ModerationLogMapper;
import com.luoxianjun.community.module.governance.mapper.ReportMapper;
import com.luoxianjun.community.module.user.entity.User;
import com.luoxianjun.community.module.user.mapper.UserMapper;
import com.luoxianjun.community.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 内容可信治理服务。
 *
 * <p>三条原则：
 * 1. 规则命中不删内容，只转人工审核（避免误伤，且保留申诉对象）；
 * 2. 待审 / 驳回内容对非作者一律等同「不存在」，不进任何公开列表；
 * 3. 每个状态变更都写审计日志，标识只解决来源可识别，不等于真实性认证。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GovernanceService {

    private final ContentReviewMapper reviewMapper;
    private final ReportMapper reportMapper;
    private final ModerationLogMapper logMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final PostCacheService postCache;
    private final ContentRuleChecker ruleChecker;

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 发帖前置规则检查，返回首个命中词；未命中返回 null */
    public String checkRules(String title, String content) {
        return ruleChecker.firstHit(title + "\n" + content);
    }

    /**
     * 隐式标识：把「谁在何时声明」写进帖子的元数据字段。
     * 用可读的结构化文本而非二进制水印，避免引入额外依赖；
     * 正式对外服务时应在上传链路补 C2PA / 水印等不可篡改凭证。
     */
    public String buildAiDeclaration(Long userId, boolean declared) {
        if (!declared) {
            return null;
        }
        return "declaredBy=" + userId + ";declaredAt=" + LocalDateTime.now().format(TS) + ";kind=AI_ASSISTED";
    }

    /**
     * 开一张审核单并把帖子置为待审。同一帖子已有待审 / 申诉中的单子时不重复开单，
     * 避免规则命中与多人举报叠加出一堆重复工单。
     */
    @Transactional
    public void openReview(Long postId, Long operatorId, String source, String reason) {
        Long existing = reviewMapper.selectCount(new LambdaQueryWrapper<ContentReview>()
                .eq(ContentReview::getPostId, postId)
                .in(ContentReview::getStatus,
                        ContentReview.STATUS_PENDING, ContentReview.STATUS_APPEALING));
        if (existing != null && existing > 0) {
            return;
        }
        Post post = new Post();
        post.setId(postId);
        post.setReviewStatus(Post.REVIEW_PENDING);
        postMapper.updateById(post);

        ContentReview review = new ContentReview();
        review.setPostId(postId);
        review.setSubmitterId(operatorId);
        review.setSource(source);
        review.setReason(reason);
        review.setStatus(ContentReview.STATUS_PENDING);
        review.setCreateTime(LocalDateTime.now());
        reviewMapper.insert(review);

        writeLog(ModerationLog.BIZ_REVIEW, review.getId(), postId, "OPEN",
                operatorId, null, ContentReview.STATUS_PENDING, reason);
        // PostVO 带 reviewStatus，旧缓存会让待审帖继续对外可见
        postCache.evict(postId);
    }

    /** 单帖治理视图（公开）：前端据此展示 AI 标识与审核状态 */
    public GovernanceDto.GovernanceVO governanceOf(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted()) || !isReadable(post)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        return new GovernanceDto.GovernanceVO(postId,
                Integer.valueOf(1).equals(post.getAiGenerated()),
                post.getReviewStatus(), post.getAiDeclaration());
    }

    /** 用户举报：只能举报自己看得见的帖子，且不允许重复举报同一帖 */
    @Transactional
    public Long submitReport(Long postId, Long reporterId, String reason) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted()) || !isReadable(post)) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        Long dup = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getPostId, postId)
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getStatus, Report.STATUS_PENDING));
        if (dup != null && dup > 0) {
            throw new BizException(ErrorCode.REPORT_DUPLICATED);
        }
        Report report = new Report();
        report.setPostId(postId);
        report.setReporterId(reporterId);
        report.setReason(reason);
        report.setStatus(Report.STATUS_PENDING);
        report.setCreateTime(LocalDateTime.now());
        reportMapper.insert(report);
        writeLog(ModerationLog.BIZ_REPORT, report.getId(), postId, "SUBMIT",
                reporterId, null, Report.STATUS_PENDING, reason);
        // 举报登记与内容处置解耦：这里只把帖子送进审核队列，由管理员判断是否下架
        openReview(postId, reporterId, ContentReview.SOURCE_REPORT, "用户举报：" + reason);
        return report.getId();
    }

    /** 作者对被驳回的内容发起申诉 */
    @Transactional
    public void appeal(Long postId, String text) {
        Post post = postMapper.selectById(postId);
        if (post == null || Integer.valueOf(1).equals(post.getDeleted())) {
            throw new BizException(ErrorCode.POST_NOT_FOUND);
        }
        Long uid = SecurityUtil.currentUserId();
        if (!post.getUserId().equals(uid)) {
            throw new BizException(ErrorCode.POST_NOT_OWNER);
        }
        ContentReview review = reviewMapper.selectOne(new LambdaQueryWrapper<ContentReview>()
                .eq(ContentReview::getPostId, postId)
                .in(ContentReview::getStatus,
                        ContentReview.STATUS_REJECTED, ContentReview.STATUS_APPEAL_UPHELD)
                .orderByDesc(ContentReview::getId)
                .last("LIMIT 1"));
        if (review == null) {
            throw new BizException(ErrorCode.APPEAL_NOT_ALLOWED);
        }
        int from = review.getStatus();
        review.setStatus(ContentReview.STATUS_APPEALING);
        review.setAppealText(text);
        reviewMapper.updateById(review);
        writeLog(ModerationLog.BIZ_REVIEW, review.getId(), postId, "APPEAL",
                uid, from, ContentReview.STATUS_APPEALING, text);
    }

    /** 管理员裁决审核单。只有待审 / 申诉中的单子可裁决 */
    @Transactional
    public void decideReview(Long reviewId, String action, String note, Long adminId) {
        ContentReview review = reviewMapper.selectById(reviewId);
        if (review == null) {
            throw new BizException(ErrorCode.REVIEW_NOT_FOUND);
        }
        int from = review.getStatus();
        int to;
        int postReview;
        switch (action) {
            case "APPROVE" -> {
                requireReviewState(from, ContentReview.STATUS_PENDING);
                to = ContentReview.STATUS_APPROVED;
                postReview = Post.REVIEW_APPROVED;
            }
            case "REJECT" -> {
                requireReviewState(from, ContentReview.STATUS_PENDING);
                to = ContentReview.STATUS_REJECTED;
                postReview = Post.REVIEW_REJECTED;
            }
            case "APPEAL_APPROVE" -> {
                requireReviewState(from, ContentReview.STATUS_APPEALING);
                to = ContentReview.STATUS_APPEAL_APPROVED;
                postReview = Post.REVIEW_APPROVED;
            }
            case "APPEAL_UPHELD" -> {
                requireReviewState(from, ContentReview.STATUS_APPEALING);
                to = ContentReview.STATUS_APPEAL_UPHELD;
                postReview = Post.REVIEW_REJECTED;
            }
            default -> throw new BizException(ErrorCode.GOVERNANCE_ACTION_INVALID);
        }
        review.setStatus(to);
        review.setReviewerId(adminId);
        review.setReviewNote(note);
        review.setReviewTime(LocalDateTime.now());
        reviewMapper.updateById(review);

        Post post = new Post();
        post.setId(review.getPostId());
        post.setReviewStatus(postReview);
        postMapper.updateById(post);
        postCache.evict(review.getPostId());
        writeLog(ModerationLog.BIZ_REVIEW, review.getId(), review.getPostId(), action,
                adminId, from, to, note);
    }

    /** 管理员处理举报：成立则确保帖子进入审核队列，不成立则直接结案 */
    @Transactional
    public void decideReport(Long reportId, String action, String note, Long adminId) {
        Report report = reportMapper.selectById(reportId);
        if (report == null) {
            throw new BizException(ErrorCode.REPORT_NOT_FOUND);
        }
        if (report.getStatus() != Report.STATUS_PENDING) {
            throw new BizException(ErrorCode.REPORT_STATE_INVALID);
        }
        int to = switch (action) {
            case "VALID" -> Report.STATUS_VALID;
            case "INVALID" -> Report.STATUS_INVALID;
            default -> throw new BizException(ErrorCode.GOVERNANCE_ACTION_INVALID);
        };
        report.setStatus(to);
        report.setHandlerId(adminId);
        report.setHandleNote(note);
        report.setHandleTime(LocalDateTime.now());
        reportMapper.updateById(report);
        writeLog(ModerationLog.BIZ_REPORT, report.getId(), report.getPostId(), action,
                adminId, Report.STATUS_PENDING, to, note);
    }

    public PageResult<GovernanceDto.ReviewVO> pageReviews(Integer status, long current, long size) {
        LambdaQueryWrapper<ContentReview> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ContentReview::getStatus, status);
        }
        wrapper.orderByAsc(ContentReview::getStatus).orderByDesc(ContentReview::getCreateTime);
        IPage<ContentReview> page = reviewMapper.selectPage(new Page<>(current, size), wrapper);
        List<GovernanceDto.ReviewVO> records = page.getRecords().stream().map(this::toReviewVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<GovernanceDto.ReportVO> pageReports(Integer status, long current, long size) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByAsc(Report::getStatus).orderByDesc(Report::getCreateTime);
        IPage<Report> page = reportMapper.selectPage(new Page<>(current, size), wrapper);
        List<GovernanceDto.ReportVO> records = page.getRecords().stream().map(this::toReportVO).toList();
        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    public PageResult<GovernanceDto.ReportVO> pageMyReports(Long userId, long current, long size) {
        IPage<Report> page = reportMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<Report>()
                        .eq(Report::getReporterId, userId)
                        .orderByDesc(Report::getCreateTime));
        return PageResult.of(page.getRecords().stream().map(this::toReportVO).toList(),
                page.getTotal(), page.getCurrent(), page.getSize());
    }

    /** 当前用户能否读到该帖（用于举报 / 治理视图，不给待审与驳回内容开口子） */
    private boolean isReadable(Post post) {
        if (!Integer.valueOf(1).equals(post.getStatus()) || !Post.isPublicReview(post.getReviewStatus())) {
            return false;
        }
        if (!Integer.valueOf(Post.VISIBILITY_PRIVATE).equals(post.getVisibility())) {
            return true;
        }
        Long uid = SecurityUtil.currentUserIdOrNull();
        return uid != null && (uid.equals(post.getUserId()) || SecurityUtil.isAdmin());
    }

    private void requireReviewState(int actual, int expected) {
        if (actual != expected) {
            throw new BizException(ErrorCode.REVIEW_STATE_INVALID);
        }
    }

    private GovernanceDto.ReviewVO toReviewVO(ContentReview r) {
        Post post = postMapper.selectById(r.getPostId());
        String title = post == null ? "（帖子已删除）" : post.getTitle();
        String author = "未知用户";
        if (post != null) {
            User u = userMapper.selectById(post.getUserId());
            if (u != null) {
                author = u.getNickname();
            }
        }
        return new GovernanceDto.ReviewVO(r.getId(), r.getPostId(), title, author, r.getSource(),
                r.getReason(), r.getStatus(), r.getReviewNote(), r.getAppealText(), r.getCreateTime());
    }

    private GovernanceDto.ReportVO toReportVO(Report r) {
        Post post = postMapper.selectById(r.getPostId());
        String title = post == null ? "（帖子已删除）" : post.getTitle();
        return new GovernanceDto.ReportVO(r.getId(), r.getPostId(), title, r.getReason(),
                r.getStatus(), r.getHandleNote(), r.getCreateTime());
    }

    private void writeLog(String bizType, Long bizId, Long postId, String action,
                          Long operatorId, Integer from, Integer to, String note) {
        ModerationLog entry = new ModerationLog();
        entry.setBizType(bizType);
        entry.setBizId(bizId);
        entry.setPostId(postId);
        entry.setAction(action);
        entry.setOperatorId(operatorId);
        entry.setFromStatus(from);
        entry.setToStatus(to);
        entry.setNote(note);
        entry.setCreateTime(LocalDateTime.now());
        logMapper.insert(entry);
    }
}
