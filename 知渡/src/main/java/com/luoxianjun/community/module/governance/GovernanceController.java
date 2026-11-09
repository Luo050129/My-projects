package com.luoxianjun.community.module.governance;

import com.luoxianjun.community.common.result.PageResult;
import com.luoxianjun.community.common.result.Result;
import com.luoxianjun.community.security.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 内容治理接口。用户侧为举报 / 申诉 / 治理视图，
 * 管理侧为审核队列与举报队列，管理侧统一用方法级鉴权兜住。
 */
@RestController
@RequiredArgsConstructor
public class GovernanceController {

    private final GovernanceService governanceService;

    /** 单帖治理信息：显式标识、审核状态、隐式声明元数据 */
    @GetMapping("/governance/posts/{postId}")
    public Result<GovernanceDto.GovernanceVO> governance(@PathVariable Long postId) {
        return Result.success(governanceService.governanceOf(postId));
    }

    /** 作者对被驳回内容发起申诉 */
    @PostMapping("/governance/posts/{postId}/appeal")
    public Result<Void> appeal(@PathVariable Long postId,
                               @Valid @RequestBody GovernanceDto.AppealRequest request) {
        governanceService.appeal(postId, request.text());
        return Result.success();
    }

    /** 举报帖子：只能举报自己看得见的内容 */
    @PostMapping("/reports")
    public Result<Long> report(@Valid @RequestBody GovernanceDto.CreateReportRequest request) {
        return Result.success(governanceService.submitReport(
                request.postId(), SecurityUtil.currentUserId(), request.reason()));
    }

    @GetMapping("/reports/mine")
    public Result<PageResult<GovernanceDto.ReportVO>> myReports(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(governanceService.pageMyReports(SecurityUtil.currentUserId(), current, size));
    }

    // ---------------- 管理端 ----------------

    @GetMapping("/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<GovernanceDto.ReviewVO>> reviews(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(governanceService.pageReviews(status, current, size));
    }

    @PostMapping("/admin/reviews/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> decideReview(@PathVariable Long id,
                                     @Valid @RequestBody GovernanceDto.ReviewDecisionRequest request) {
        governanceService.decideReview(id, request.action(), request.note(), SecurityUtil.currentUserId());
        return Result.success();
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<GovernanceDto.ReportVO>> reports(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return Result.success(governanceService.pageReports(status, current, size));
    }

    @PostMapping("/admin/reports/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> decideReport(@PathVariable Long id,
                                     @Valid @RequestBody GovernanceDto.ReportDecisionRequest request) {
        governanceService.decideReport(id, request.action(), request.note(), SecurityUtil.currentUserId());
        return Result.success();
    }
}
