package com.luoxianjun.community.common.exception;

import lombok.Getter;

/**
 * 业务错误码枚举。约定：10xxx 通用 / 20xxx 用户 / 30xxx 内容 / 40xxx 通知 / 50xxx AI。
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "ok"),
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),

    PARAM_INVALID(10001, "参数校验失败"),
    UNAUTHORIZED(10002, "未登录或登录已过期"),
    FORBIDDEN(10003, "无权限访问该资源"),

    USER_NOT_FOUND(20001, "用户不存在"),
    USERNAME_EXISTS(20002, "用户名已存在"),
    PASSWORD_WRONG(20003, "用户名或密码错误"),

    POST_NOT_FOUND(30001, "帖子不存在"),
    POST_NOT_OWNER(30002, "只能操作自己发布的帖子"),
    REVIEW_NOT_FOUND(30003, "审核单不存在"),
    REVIEW_STATE_INVALID(30004, "审核单当前状态不允许该操作"),
    REPORT_NOT_FOUND(30005, "举报不存在"),
    REPORT_STATE_INVALID(30006, "该举报已处理"),
    APPEAL_NOT_ALLOWED(30007, "当前内容无可申诉的驳回记录"),
    GOVERNANCE_ACTION_INVALID(30008, "处置动作不合法"),
    REPORT_DUPLICATED(30009, "你已举报过该内容，请等待处理"),

    NOTIFY_NOT_FOUND(40001, "通知不存在"),

    AI_SERVICE_UNAVAILABLE(50001, "AI 服务暂不可用"),

    FILE_EMPTY(60001, "文件为空"),
    FILE_TYPE_UNSUPPORTED(60002, "仅支持 PNG / JPEG / GIF / WEBP 图片"),
    FILE_TOO_LARGE(60003, "图片大小超出限制"),
    FILE_SAVE_FAILED(60004, "文件保存失败，请稍后重试");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
