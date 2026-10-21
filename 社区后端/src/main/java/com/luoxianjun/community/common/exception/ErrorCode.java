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

    NOTIFY_NOT_FOUND(40001, "通知不存在"),

    AI_SERVICE_UNAVAILABLE(50001, "AI 服务暂不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
