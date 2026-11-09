package com.luoxianjun.community.module.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** 治理模块请求 / 响应类型集中放置，避免大量只有一个 record 的小文件。 */
public final class GovernanceDto {

    private GovernanceDto() {
    }

    public record CreateReportRequest(
            @NotNull(message = "帖子 id 不能为空") Long postId,
            @NotBlank(message = "举报理由不能为空")
            @Size(max = 500, message = "举报理由过长") String reason) {
    }

    /** action: APPROVE / REJECT / APPEAL_APPROVE / APPEAL_UPHELD */
    public record ReviewDecisionRequest(
            @NotBlank(message = "处置动作不能为空") String action,
            @Size(max = 500, message = "处置说明过长") String note) {
    }

    /** action: VALID / INVALID */
    public record ReportDecisionRequest(
            @NotBlank(message = "处置动作不能为空") String action,
            @Size(max = 500, message = "处置说明过长") String note) {
    }

    public record AppealRequest(
            @NotBlank(message = "申诉说明不能为空")
            @Size(max = 500, message = "申诉说明过长") String text) {
    }

    public record ReviewVO(
            Long id,
            Long postId,
            String title,
            String authorName,
            String source,
            String reason,
            Integer status,
            String reviewNote,
            String appealText,
            LocalDateTime createTime) {
    }

    public record ReportVO(
            Long id,
            Long postId,
            String title,
            String reason,
            Integer status,
            String handleNote,
            LocalDateTime createTime) {
    }

    /** 单帖治理视图：供前端展示显式标识与审核状态 */
    public record GovernanceVO(
            Long postId,
            Boolean aiGenerated,
            Integer reviewStatus,
            String aiDeclaration) {
    }
}
